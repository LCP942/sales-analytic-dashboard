package com.lp.salesdashboard.dto;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, String category, BigDecimal price) {}
