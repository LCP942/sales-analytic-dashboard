package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

public record WeekdayStatDto(String day, long orderCount, BigDecimal revenue) {}
