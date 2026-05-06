package com.lp.salesdashboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull Long productId,
        @Min(1) int quantity,
        @NotNull @Positive BigDecimal unitPrice) {}
