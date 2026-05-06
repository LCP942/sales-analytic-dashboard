package com.lp.salesdashboard.projection;

import java.math.BigDecimal;

public interface CustomerSummaryProjection {
    Long getId();
    String getName();
    String getEmail();
    String getCity();
    Long getOrderCount();
    BigDecimal getLifetimeValue();
}
