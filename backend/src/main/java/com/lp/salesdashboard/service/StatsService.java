package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.*;

import java.util.EnumMap;
import com.lp.salesdashboard.repository.OrderItemRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final int TOP_PRODUCTS_LIMIT = 10;

    private final SalesOrderRepository orderRepo;
    private final OrderItemRepository itemRepo;

    public StatsService(SalesOrderRepository orderRepo, OrderItemRepository itemRepo) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
    }

    // -------------------------------------------------------------------------
    // KPIs
    // -------------------------------------------------------------------------

    public KpiMetricsDto getKpis(LocalDate from, LocalDate to) {
        KpiRawDto current = normalize(orderRepo.findKpiMetrics(from, to));

        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(rangeDays - 1);
        KpiRawDto previous = normalize(orderRepo.findKpiMetrics(prevFrom, prevTo));

        return new KpiMetricsDto(
                current.revenue(),
                current.orderCount(),
                current.avgOrderValue(),
                growthRate(previous.revenue(), current.revenue()),
                growthRate(BigDecimal.valueOf(previous.orderCount()), BigDecimal.valueOf(current.orderCount())),
                growthRate(previous.avgOrderValue(), current.avgOrderValue())
        );
    }

    /** Replaces null aggregates from an empty result set with zero values. */
    private KpiRawDto normalize(KpiRawDto raw) {
        if (raw == null) return new KpiRawDto(BigDecimal.ZERO, 0L);
        return new KpiRawDto(
                raw.revenue() != null ? raw.revenue() : BigDecimal.ZERO,
                raw.orderCount() != null ? raw.orderCount() : 0L
        );
    }

    // -------------------------------------------------------------------------
    // Revenue over time
    // -------------------------------------------------------------------------

    public List<RevenuePointDto> getRevenueOverTime(LocalDate from, LocalDate to) {
        List<RevenuePointDto> daily = orderRepo.findDailyRevenue(from, to);
        long days = ChronoUnit.DAYS.between(from, to);

        if (days <= 31) return daily;
        if (days <= 90) return groupRevenueByWeek(daily);
        return groupRevenueByMonth(daily);
    }

    // -------------------------------------------------------------------------
    // Order count over time
    // -------------------------------------------------------------------------

    public List<RevenuePointDto> getOrderCountOverTime(LocalDate from, LocalDate to) {
        List<Object[]> daily = orderRepo.findDailyOrderCount(from, to);
        long days = ChronoUnit.DAYS.between(from, to);

        List<RevenuePointDto> points = daily.stream()
                .map(row -> new RevenuePointDto(
                        row[0].toString(),
                        BigDecimal.valueOf(((Number) row[1]).longValue())))
                .toList();

        if (days <= 31) return points;
        if (days <= 90) return groupRevenueByWeek(points);
        return groupRevenueByMonth(points);
    }

    // -------------------------------------------------------------------------
    // Top products
    // -------------------------------------------------------------------------

    public List<TopProductDto> getTopProducts(LocalDate from, LocalDate to) {
        return itemRepo.findTopProducts(from, to, PageRequest.of(0, TOP_PRODUCTS_LIMIT))
                .stream()
                .map(row -> new TopProductDto(
                        (String) row[0],
                        new BigDecimal(row[1].toString())))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Orders by category
    // -------------------------------------------------------------------------

    public List<CategoryBreakdownDto> getOrdersByCategory(LocalDate from, LocalDate to) {
        return itemRepo.findOrdersByCategory(from, to)
                .stream()
                .map(row -> new CategoryBreakdownDto(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Orders by weekday
    // -------------------------------------------------------------------------

    public List<WeekdayStatDto> getOrdersByWeekday(LocalDate from, LocalDate to) {
        Map<DayOfWeek, long[]> counts = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, BigDecimal> revenues = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            counts.put(d, new long[]{0});
            revenues.put(d, BigDecimal.ZERO);
        }

        for (Object[] row : orderRepo.findDailyStats(from, to)) {
            LocalDate date = (LocalDate) row[0];
            DayOfWeek dow = date.getDayOfWeek();
            counts.get(dow)[0] += ((Number) row[1]).longValue();
            revenues.merge(dow, new BigDecimal(row[2].toString()), BigDecimal::add);
        }

        // Return Mon → Sun order
        return List.of(DayOfWeek.values()).stream()
                .map(d -> new WeekdayStatDto(
                        d.name().charAt(0) + d.name().substring(1).toLowerCase(),
                        counts.get(d)[0],
                        revenues.get(d)))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double growthRate(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /** Groups daily revenue points into ISO week buckets (label = Monday of the week). */
    private List<RevenuePointDto> groupRevenueByWeek(List<RevenuePointDto> daily) {
        Map<String, BigDecimal> byWeek = new TreeMap<>();
        for (RevenuePointDto p : daily) {
            LocalDate date = LocalDate.parse(p.label());
            LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            String key = monday.format(DateTimeFormatter.ISO_LOCAL_DATE);
            byWeek.merge(key, p.revenue(), BigDecimal::add);
        }
        return byWeek.entrySet().stream()
                .map(e -> new RevenuePointDto(e.getKey(), e.getValue()))
                .toList();
    }

    /** Groups daily revenue points into month buckets (label = YYYY-MM). */
    private List<RevenuePointDto> groupRevenueByMonth(List<RevenuePointDto> daily) {
        Map<String, BigDecimal> byMonth = new TreeMap<>();
        for (RevenuePointDto p : daily) {
            String key = p.label().substring(0, 7); // "YYYY-MM"
            byMonth.merge(key, p.revenue(), BigDecimal::add);
        }
        return byMonth.entrySet().stream()
                .map(e -> new RevenuePointDto(e.getKey(), e.getValue()))
                .toList();
    }
}
