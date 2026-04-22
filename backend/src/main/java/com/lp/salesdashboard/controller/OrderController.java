package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.OrderDetailDto;
import com.lp.salesdashboard.dto.OrderSummaryDto;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public Page<OrderSummaryDto> getOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "") String customer,
            @RequestParam(required = false) String statuses,
            @PageableDefault(size = 10, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getOrders(from, to, customer, parseStatuses(statuses), pageable);
    }

    @GetMapping("/{id}")
    public OrderDetailDto getOrder(@PathVariable Long id) {
        return service.getOrder(id);
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
}
