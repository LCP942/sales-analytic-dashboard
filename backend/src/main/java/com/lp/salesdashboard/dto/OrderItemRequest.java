package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

/** A single line item inside an OrderCreateRequest. */
public record OrderItemRequest(Long productId, int quantity, BigDecimal unitPrice) {}
