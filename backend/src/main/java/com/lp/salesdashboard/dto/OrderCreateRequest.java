package com.lp.salesdashboard.dto;

import com.lp.salesdashboard.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderCreateRequest(
        @NotNull Long customerId,
        LocalDate orderDate,
        OrderStatus status,
        String paymentMethod,
        BigDecimal shippingAmount,
        @NotNull @NotEmpty @Valid List<OrderItemRequest> items) {}
