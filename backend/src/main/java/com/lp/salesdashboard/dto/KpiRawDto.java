package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

/**
 * Raw aggregate returned directly from a JPQL constructor expression.
 * {@code revenue} may be {@code null} when the date range contains no orders.
 */
public record KpiRawDto(
        BigDecimal revenue,
        Long orderCount
) {
    /** Returns average order value, or zero if there are no orders. */
    public BigDecimal avgOrderValue() {
        if (orderCount == null || orderCount == 0 || revenue == null) return BigDecimal.ZERO;
        return revenue.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);
    }
}
