package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.entity.OrderItem;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.projection.CustomerSummaryProjection;
import com.lp.salesdashboard.service.CustomerService;
import com.lp.salesdashboard.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService    orderService;
    private final CustomerService customerService;

    @GetMapping
    public Page<OrderSummaryDto> getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "") String customer,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "") String categories,
            @RequestParam(defaultValue = "") String product,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.getOrders(from, to, customer, parseStatuses(statuses), minAmount, maxAmount,
                parseCategories(categories), product, pageable)
                .map(OrderController::toSummary);
    }

    @GetMapping("/{id}")
    public OrderDetailDto getOrder(@PathVariable Long id) {
        return toDetail(orderService.getOrder(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailDto createOrder(@Valid @RequestBody OrderCreateRequest req) {
        return toDetail(orderService.createOrder(req));
    }

    private OrderDetailDto toDetail(SalesOrder order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(OrderController::toOrderItemDto)
                .toList();
        CustomerDto customerDto = toCustomerDto(customerService.getCustomer(order.getCustomer().getId()));
        return new OrderDetailDto(
                order.getId(), order.getOrderDate(), order.getTotalAmount(), order.getStatus(),
                customerDto, items.size(), items,
                order.getTotalAmount().subtract(order.getShippingAmount()),
                order.getShippingAmount(), order.getPaymentMethod());
    }

    private static OrderSummaryDto toSummary(SalesOrder o) {
        return new OrderSummaryDto(o.getId(), o.getOrderDate(), o.getCustomer().getName(),
                o.getTotalAmount(), o.getStatus());
    }

    private static OrderItemDto toOrderItemDto(OrderItem i) {
        return new OrderItemDto(i.getProduct().getName(), i.getProduct().getCategory(),
                i.getQuantity(), i.getUnitPrice(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
    }

    private static CustomerDto toCustomerDto(CustomerSummaryProjection p) {
        return new CustomerDto(p.getId(), p.getName(), p.getEmail(), p.getCity(),
                p.getOrderCount().intValue(), p.getLifetimeValue());
    }

    private List<OrderStatus> parseStatuses(String statuses) {
        if (statuses == null || statuses.isBlank()) return List.of();
        return Arrays.stream(statuses.split(","))
                .map(String::trim)
                .map(s -> {
                    try {
                        return OrderStatus.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + s);
                    }
                })
                .toList();
    }

    private List<String> parseCategories(String categories) {
        if (categories == null || categories.isBlank()) return List.of();
        return Arrays.stream(categories.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
