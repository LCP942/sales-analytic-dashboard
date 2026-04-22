package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String productName,
        String category,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
