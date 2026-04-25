package com.lp.salesdashboard.projection;

import java.time.LocalDate;

public interface DailyOrderCountProjection {
    LocalDate getOrderDate();
    Long getOrderCount();
}
