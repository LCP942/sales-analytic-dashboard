package com.lp.salesdashboard.dto;

import com.lp.salesdashboard.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderSummaryDto(
        Long id,
        LocalDate orderDate,
        String customerName,
        BigDecimal totalAmount,
        OrderStatus status
) {}
