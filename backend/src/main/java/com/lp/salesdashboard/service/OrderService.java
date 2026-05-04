package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.Product;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.repository.ProductRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import com.lp.salesdashboard.repository.h2.OrderH2Dao;
import com.lp.salesdashboard.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles order retrieval and creation across MySQL (read) and H2 (write).
 *
 * <p>Listing strategy: H2 orders (newest first) are prepended to the MySQL
 * page for page 0. Subsequent pages query MySQL with an offset that accounts
 * for the H2 count, keeping the combined total consistent.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final SalesOrderRepository mysqlOrders;
    private final ProductRepository    mysqlProducts;
    private final OrderH2Dao           h2Orders;
    private final CustomerService      customerService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public Page<OrderSummaryDto> getOrders(
            LocalDate from, LocalDate to,
            String customer, List<OrderStatus> statuses,
            BigDecimal minAmount, BigDecimal maxAmount,
            List<String> categories, String product,
            Pageable pageable) {

        Specification<SalesOrder> spec = Specification
                .where(OrderSpecifications.betweenDates(from, to))
                .and(OrderSpecifications.customerContains(customer))
                .and(OrderSpecifications.statusIn(statuses))
                .and(OrderSpecifications.amountBetween(minAmount, maxAmount))
                .and(OrderSpecifications.categoryIn(categories))
                .and(OrderSpecifications.productContains(product));

        // Filter H2 orders in memory
        List<OrderSummaryDto> h2All = h2Orders.findAllSummaries().stream()
                .filter(o -> from == null || !o.orderDate().isBefore(from))
                .filter(o -> to   == null || !o.orderDate().isAfter(to))
                .filter(o -> customer == null || customer.isBlank()
                        || o.customerName().toLowerCase().contains(customer.toLowerCase()))
                .filter(o -> statuses == null || statuses.isEmpty() || statuses.contains(o.status()))
                .filter(o -> minAmount == null || o.totalAmount().compareTo(minAmount) >= 0)
                .filter(o -> maxAmount == null || o.totalAmount().compareTo(maxAmount) <= 0)
                // Note: category and product filters are MySQL-specific; H2 orders pass through
                .toList();

        int h2Count  = h2All.size();
        int pageNum  = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int virtualStart = pageNum * pageSize;
        int virtualEnd   = virtualStart + pageSize;

        // H2 items that fall in this virtual page window
        List<OrderSummaryDto> h2InPage = h2All.subList(
                Math.min(virtualStart, h2Count),
                Math.min(virtualEnd,   h2Count));

        // MySQL: fetch with offset adjusted for H2 rows
        int mysqlSlots  = pageSize - h2InPage.size();
        long mysqlStart = Math.max(0L, virtualStart - h2Count);

        Page<SalesOrder> mysqlPage;
        if (mysqlSlots > 0) {
            Pageable mysqlPageable = PageRequest.of(
                    (int) (mysqlStart / pageSize), pageSize, pageable.getSort());
            mysqlPage = mysqlOrders.findAll(spec, mysqlPageable);
        } else {
            // No room for MySQL rows on this page; still fetch to get the total
            mysqlPage = mysqlOrders.findAll(spec, PageRequest.of(0, 1, pageable.getSort()));
        }

        List<OrderSummaryDto> mysqlDtos = mysqlPage.getContent().stream()
                .limit(mysqlSlots)
                .map(o -> new OrderSummaryDto(
                        o.getId(), o.getOrderDate(),
                        o.getCustomer().getName(),
                        o.getTotalAmount(), o.getStatus()))
                .toList();

        List<OrderSummaryDto> content = new ArrayList<>(h2InPage);
        content.addAll(mysqlDtos);

        return new PageImpl<>(content, pageable, h2Count + mysqlPage.getTotalElements());
    }

    /**
     * Loads a full order detail. Tries H2 first (free in-memory lookup),
     * then falls back to MySQL.
     */
    public OrderDetailDto getOrder(Long id) {
        return h2Orders.findById(id)
                .map(this::buildH2OrderDetail)
                .orElseGet(() -> buildMysqlOrderDetail(id));
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    @Transactional
    public OrderDetailDto createOrder(OrderCreateRequest req) {
        if (req.items() == null || req.items().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must have at least one item");

        // Resolve customer (MySQL or H2)
        customerService.findCustomerById(req.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Customer not found: " + req.customerId()));

        BigDecimal shipping = req.shippingAmount() != null ? req.shippingAmount() : BigDecimal.ZERO;
        BigDecimal itemsTotal = req.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = itemsTotal.add(shipping);

        LocalDate orderDate   = req.orderDate() != null ? req.orderDate() : LocalDate.now();
        OrderStatus status    = req.status()    != null ? req.status()    : OrderStatus.PENDING;
        String paymentMethod  = req.paymentMethod() != null && !req.paymentMethod().isBlank()
                ? req.paymentMethod() : "Credit Card";

        Long orderId = h2Orders.saveOrder(orderDate, total, req.customerId(),
                status, paymentMethod, shipping, req.items());

        return getOrder(orderId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OrderDetailDto buildMysqlOrderDetail(Long id) {
        SalesOrder order = mysqlOrders.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));

        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProduct().getName(), i.getProduct().getCategory(),
                        i.getQuantity(), i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();

        Long customerId = order.getCustomer().getId();
        long orderCount = mysqlOrders.countByCustomerId(customerId) + h2Orders.countByCustomerId(customerId);
        BigDecimal lv   = mysqlOrders.sumTotalByCustomerId(customerId).add(h2Orders.sumTotalByCustomerId(customerId));

        CustomerDto customerDto = new CustomerDto(customerId,
                order.getCustomer().getName(), order.getCustomer().getEmail(),
                order.getCustomer().getCity(), (int) orderCount, lv);

        return new OrderDetailDto(
                order.getId(), order.getOrderDate(), order.getTotalAmount(), order.getStatus(),
                customerDto, items.size(), items,
                order.getTotalAmount().subtract(order.getShippingAmount()),
                order.getShippingAmount(), order.getPaymentMethod());
    }

    private OrderDetailDto buildH2OrderDetail(OrderH2Dao.OrderH2Row row) {
        List<OrderH2Dao.ItemRow> itemRows = h2Orders.findItemsByOrderId(row.id());
        List<OrderItemDto> items = itemRows.stream().map(i -> {
            Product product = mysqlProducts.findById(i.productId()).orElse(null);
            String productName = product != null ? product.getName()     : "Unknown Product";
            String category    = product != null ? product.getCategory() : "Unknown";
            return new OrderItemDto(productName, category, i.quantity(), i.unitPrice(),
                    i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())));
        }).toList();

        Customer customer = customerService.findCustomerById(row.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer not found: " + row.customerId()));

        long orderCount = mysqlOrders.countByCustomerId(row.customerId())
                + h2Orders.countByCustomerId(row.customerId());
        BigDecimal lv   = mysqlOrders.sumTotalByCustomerId(row.customerId())
                .add(h2Orders.sumTotalByCustomerId(row.customerId()));

        CustomerDto customerDto = new CustomerDto(customer.getId(), customer.getName(),
                customer.getEmail(), customer.getCity(), (int) orderCount, lv);

        return new OrderDetailDto(
                row.id(), row.orderDate(), row.totalAmount(), row.status(),
                customerDto, items.size(), items,
                row.totalAmount().subtract(row.shippingAmount()),
                row.shippingAmount(), row.paymentMethod());
    }
}
