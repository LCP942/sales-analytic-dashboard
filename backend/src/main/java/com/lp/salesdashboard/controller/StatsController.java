package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/kpis")
    public KpiMetricsDto getKpis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getKpis(from, to);
    }

    @GetMapping("/revenue-over-time")
    public List<RevenuePointDto> getRevenueOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getRevenueOverTime(from, to);
    }

    @GetMapping("/orders-over-time")
    public List<RevenuePointDto> getOrderCountOverTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getOrderCountOverTime(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductDto> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getTopProducts(from, to);
    }

    @GetMapping("/orders-by-category")
    public List<CategoryBreakdownDto> getOrdersByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getOrdersByCategory(from, to);
    }
}
