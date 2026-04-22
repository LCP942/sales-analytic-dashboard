package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

public record KpiMetricsDto(
        BigDecimal revenue,
        long orderCount,
        BigDecimal avgOrderValue,
        double revenueGrowth,
        double orderGrowth,
        double avgOrderGrowth
) {}
