package com.lp.salesdashboard.dto;

import com.lp.salesdashboard.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderDetailDto(
        Long id,
        LocalDate orderDate,
        BigDecimal totalAmount,
        OrderStatus status,
        CustomerDto customer,
        int itemCount,
        List<OrderItemDto> items,
        BigDecimal subtotal,
        BigDecimal shippingAmount,
        String paymentMethod
) {}
