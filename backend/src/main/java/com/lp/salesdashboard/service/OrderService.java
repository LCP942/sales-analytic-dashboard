package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.CustomerDto;
import com.lp.salesdashboard.dto.OrderDetailDto;
import com.lp.salesdashboard.dto.OrderItemDto;
import com.lp.salesdashboard.dto.OrderSummaryDto;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import com.lp.salesdashboard.specification.OrderSpecifications;
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

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final SalesOrderRepository repo;

    public OrderService(SalesOrderRepository repo) {
        this.repo = repo;
    }

    public Page<OrderSummaryDto> getOrders(LocalDate from, LocalDate to, String customer, List<OrderStatus> statuses, Pageable pageable) {
        Specification<SalesOrder> spec = Specification
                .where(OrderSpecifications.betweenDates(from, to))
                .and(OrderSpecifications.customerContains(customer))
                .and(OrderSpecifications.statusIn(statuses));

        return repo.findAll(spec, pageable)
                .map(o -> new OrderSummaryDto(
                        o.getId(),
                        o.getOrderDate(),
                        o.getCustomer().getName(),
                        o.getTotalAmount(),
                        o.getStatus()));
    }

    public OrderDetailDto getOrder(Long id) {
        SalesOrder order = repo.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));

        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProduct().getName(),
                        i.getProduct().getCategory(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();

        Long customerId = order.getCustomer().getId();
        int orderCount = (int) repo.countByCustomerId(customerId);
        BigDecimal lifetimeValue = repo.sumTotalByCustomerId(customerId);

        CustomerDto customer = new CustomerDto(
                customerId,
                order.getCustomer().getName(),
                order.getCustomer().getEmail(),
                order.getCustomer().getCity(),
                orderCount,
                lifetimeValue);

        BigDecimal subtotal = order.getTotalAmount().subtract(order.getShippingAmount());

        return new OrderDetailDto(
                order.getId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus(),
                customer,
                items.size(),
                items,
                subtotal,
                order.getShippingAmount(),
                order.getPaymentMethod());
    }
}
