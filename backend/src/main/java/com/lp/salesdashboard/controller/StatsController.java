package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.projection.CategoryProjection;
import com.lp.salesdashboard.projection.TopProductProjection;
import com.lp.salesdashboard.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

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
        return statsService.getTopProducts(from, to).stream()
                .map(StatsController::toDto)
                .toList();
    }

    @GetMapping("/orders-by-weekday")
    public List<WeekdayStatDto> getOrdersByWeekday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getOrdersByWeekday(from, to);
    }

    @GetMapping("/orders-by-category")
    public List<CategoryBreakdownDto> getOrdersByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statsService.getOrdersByCategory(from, to).stream()
                .map(StatsController::toDto)
                .toList();
    }

    private static TopProductDto toDto(TopProductProjection p) {
        return new TopProductDto(p.getName(), p.getRevenue());
    }

    private static CategoryBreakdownDto toDto(CategoryProjection p) {
        return new CategoryBreakdownDto(p.getCategory(), p.getItemCount());
    }
}
