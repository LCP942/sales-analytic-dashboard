package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.OrderCreateRequest;
import com.lp.salesdashboard.dto.OrderItemRequest;
import com.lp.salesdashboard.entity.*;
import com.lp.salesdashboard.repository.ProductRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import com.lp.salesdashboard.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository    productRepository;
    private final CustomerService      customerService;

    public Page<SalesOrder> getOrders(
            LocalDate from, LocalDate to,
            String customer, List<OrderStatus> statuses,
            BigDecimal minAmount, BigDecimal maxAmount,
            List<String> categories, String product,
            String ip, Pageable pageable) {

        Specification<SalesOrder> spec = Specification
                .where(OrderSpecifications.betweenDates(from, to))
                .and(OrderSpecifications.customerContains(customer))
                .and(OrderSpecifications.statusIn(statuses))
                .and(OrderSpecifications.amountBetween(minAmount, maxAmount))
                .and(OrderSpecifications.categoryIn(categories))
                .and(OrderSpecifications.productContains(product))
                .and(OrderSpecifications.visibleToIp(ip));

        return salesOrderRepository.findAll(spec, pageable);
    }

    public SalesOrder getOrder(Long id, String ip) {
        SalesOrder order = salesOrderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
        if (order.getCreatorIp() != null && !order.getCreatorIp().equals(ip)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id);
        }
        return order;
    }

    @Transactional
    public SalesOrder createOrder(OrderCreateRequest req, String ip) {
        Customer customer = customerService.findCustomerById(req.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Customer not found: " + req.customerId()));

        BigDecimal shipping = req.shippingAmount() != null ? req.shippingAmount() : BigDecimal.ZERO;
        BigDecimal itemsTotal = req.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SalesOrder order = new SalesOrder();
        order.setCustomer(customer);
        order.setOrderDate(req.orderDate() != null ? req.orderDate() : LocalDate.now());
        order.setStatus(req.status() != null ? req.status() : OrderStatus.PENDING);
        order.setPaymentMethod(req.paymentMethod() != null && !req.paymentMethod().isBlank()
                ? req.paymentMethod() : "Credit Card");
        order.setShippingAmount(shipping);
        order.setTotalAmount(itemsTotal.add(shipping));
        order.setCreatorIp(ip);

        for (OrderItemRequest itemReq : req.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Product not found: " + itemReq.productId())));
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            order.getItems().add(item);
        }

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Order created: id={}, customerId={}, items={}, total={}",
                saved.getId(), req.customerId(), req.items().size(), saved.getTotalAmount());
        return getOrder(saved.getId(), ip);
    }
}
