package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

/**
 * Raw aggregate returned directly from JPQL constructor expression.
 * Java 21 Record — immutable by design, equals/hashCode/toString generated.
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
