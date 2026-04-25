package com.lp.salesdashboard.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyStatProjection {
    LocalDate getOrderDate();
    Long getOrderCount();
    BigDecimal getRevenue();
}
