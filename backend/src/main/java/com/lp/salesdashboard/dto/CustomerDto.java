package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

public record CustomerDto(Long id, String name, String email, String city, int orderCount, BigDecimal lifetimeValue) {}
