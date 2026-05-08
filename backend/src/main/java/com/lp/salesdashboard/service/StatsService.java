package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.projection.CategoryProjection;
import com.lp.salesdashboard.projection.DailyStatProjection;
import com.lp.salesdashboard.projection.TopProductProjection;

import java.util.EnumMap;
import com.lp.salesdashboard.repository.OrderItemRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Stream;

/** Provides aggregated statistics for the dashboard: KPIs, revenue trends, top products, and order breakdowns. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsService {

    private static final int TOP_PRODUCTS_LIMIT = 10;

    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;

    // -------------------------------------------------------------------------
    // KPIs
    // -------------------------------------------------------------------------

    /**
     * Returns KPI metrics for the given range, with growth rates compared
     * to the immediately preceding period of equal length.
     */
    public KpiMetricsDto getKpis(LocalDate from, LocalDate to) {
        KpiRawDto current = normalize(salesOrderRepository.findKpiMetrics(from, to));

        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(rangeDays - 1);
        KpiRawDto previous = normalize(salesOrderRepository.findKpiMetrics(prevFrom, prevTo));

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

    /**
     * Returns revenue aggregated at a granularity that fits the range:
     * <= 31 days -> daily, <= 90 days -> weekly (Monday-anchored), otherwise -> monthly (YYYY-MM).
     */
    public List<RevenuePointDto> getRevenueOverTime(LocalDate from, LocalDate to) {
        List<RevenuePointDto> daily = salesOrderRepository.findDailyRevenue(from, to);
        long days = ChronoUnit.DAYS.between(from, to);

        if (days <= 31) return daily;
        if (days <= 90) return groupRevenueByWeek(daily);
        return groupRevenueByMonth(daily);
    }

    // -------------------------------------------------------------------------
    // Order count over time
    // -------------------------------------------------------------------------

    /** Same granularity logic as {@link #getRevenueOverTime}, counting orders instead of summing revenue. */
    public List<RevenuePointDto> getOrderCountOverTime(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);

        List<RevenuePointDto> points = salesOrderRepository.findDailyOrderCount(from, to).stream()
                .map(p -> new RevenuePointDto(
                        p.getOrderDate().toString(),
                        BigDecimal.valueOf(p.getOrderCount())))
                .toList();

        if (days <= 31) return points;
        if (days <= 90) return groupRevenueByWeek(points);
        return groupRevenueByMonth(points);
    }

    // -------------------------------------------------------------------------
    // Top products
    // -------------------------------------------------------------------------

    public List<TopProductProjection> getTopProducts(LocalDate from, LocalDate to) {
        return orderItemRepository.findTopProducts(from, to, PageRequest.of(0, TOP_PRODUCTS_LIMIT));
    }

    // -------------------------------------------------------------------------
    // Orders by category
    // -------------------------------------------------------------------------

    public List<CategoryProjection> getOrdersByCategory(LocalDate from, LocalDate to) {
        return orderItemRepository.findOrdersByCategory(from, to);
    }

    // -------------------------------------------------------------------------
    // Orders by weekday
    // -------------------------------------------------------------------------

    /** Aggregates orders by day-of-week across the full range. Days with no orders are included with zero counts. */
    public List<WeekdayStatDto> getOrdersByWeekday(LocalDate from, LocalDate to) {
        Map<DayOfWeek, Long> counts = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, BigDecimal> revenues = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            counts.put(d, 0L);
            revenues.put(d, BigDecimal.ZERO);
        }

        for (DailyStatProjection p : salesOrderRepository.findDailyStats(from, to)) {
            DayOfWeek dow = p.getOrderDate().getDayOfWeek();
            counts.merge(dow, p.getOrderCount(), Long::sum);
            revenues.merge(dow, p.getRevenue(), BigDecimal::add);
        }

        return Stream.of(DayOfWeek.values())
                .map(d -> new WeekdayStatDto(
                        d.name().charAt(0) + d.name().substring(1).toLowerCase(),
                        counts.get(d),
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
