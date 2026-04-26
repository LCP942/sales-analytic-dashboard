package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.dto.KpiRawDto;
import com.lp.salesdashboard.dto.RevenuePointDto;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.projection.DailyOrderCountProjection;
import com.lp.salesdashboard.projection.DailyStatProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    /** Overrides the default to eagerly load {@code customer} via an EntityGraph, preventing N+1 queries when mapping the results page. */
    @Override
    @EntityGraph(attributePaths = "customer")
    Page<SalesOrder> findAll(Specification<SalesOrder> spec, Pageable pageable);

    /**
     * Returns total revenue and order count for the given date range.
     * {@code revenue} in the result may be {@code null} if no orders exist in the range.
     */
    @Query("""
            SELECT new com.lp.salesdashboard.dto.KpiRawDto(
                SUM(o.totalAmount),
                COUNT(o)
            )
            FROM SalesOrder o
            WHERE o.orderDate BETWEEN :from AND :to
            """)
    KpiRawDto findKpiMetrics(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT new com.lp.salesdashboard.dto.RevenuePointDto(
                CAST(o.orderDate AS string),
                SUM(o.totalAmount)
            )
            FROM SalesOrder o
            WHERE o.orderDate BETWEEN :from AND :to
            GROUP BY o.orderDate
            ORDER BY o.orderDate
            """)
    List<RevenuePointDto> findDailyRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT o.orderDate AS orderDate, COUNT(o) AS orderCount
            FROM SalesOrder o
            WHERE o.orderDate BETWEEN :from AND :to
            GROUP BY o.orderDate
            ORDER BY o.orderDate
            """)
    List<DailyOrderCountProjection> findDailyOrderCount(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Fetches the order with customer, items, and products in a single query. Use this for the detail view to avoid lazy-loading round-trips. */
    @Query("SELECT o FROM SalesOrder o JOIN FETCH o.customer JOIN FETCH o.items i JOIN FETCH i.product WHERE o.id = :id")
    Optional<SalesOrder> findWithItemsById(@Param("id") Long id);

    @Query("""
            SELECT o.orderDate AS orderDate, COUNT(o) AS orderCount, SUM(o.totalAmount) AS revenue
            FROM SalesOrder o
            WHERE o.orderDate BETWEEN :from AND :to
            GROUP BY o.orderDate
            """)
    List<DailyStatProjection> findDailyStats(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Returns the total number of orders placed by a customer, used for the customer summary panel. */
    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE o.customer.id = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);

    /** Returns the lifetime revenue for a customer. {@code COALESCE} ensures 0 is returned rather than {@code null} when no orders exist. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM SalesOrder o WHERE o.customer.id = :customerId")
    BigDecimal sumTotalByCustomerId(@Param("customerId") Long customerId);
}
