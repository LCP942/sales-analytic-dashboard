package com.lp.salesdashboard.dto;

import com.lp.salesdashboard.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for POST /api/orders.
 * totalAmount is computed server-side from items + shippingAmount.
 */
public record OrderCreateRequest(
        Long customerId,
        LocalDate orderDate,
        OrderStatus status,
        String paymentMethod,
        BigDecimal shippingAmount,
        List<OrderItemRequest> items) {}
