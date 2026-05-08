package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.OrderItem;
import com.lp.salesdashboard.projection.CategoryProjection;
import com.lp.salesdashboard.projection.TopProductProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Ranks products by total revenue ({@code quantity * unit_price}) within the date range.
     * Native query - JPQL does not support the {@code GROUP BY p.id} with Pageable in all JPA providers.
     * {@code Pageable} controls the result count (use {@code PageRequest.of(0, n)}).
     */
    @Query(value = """
            SELECT p.name AS name, SUM(oi.quantity * oi.unit_price) AS revenue
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN sales_orders o ON oi.order_id = o.id
            WHERE o.order_date BETWEEN :from AND :to
            GROUP BY p.id, p.name
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<TopProductProjection> findTopProducts(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /** Aggregates item quantities sold per product category within the date range, ordered by volume descending. */
    @Query(value = """
            SELECT p.category AS category, SUM(oi.quantity) AS itemCount
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN sales_orders o ON oi.order_id = o.id
            WHERE o.order_date BETWEEN :from AND :to
            GROUP BY p.category
            ORDER BY itemCount DESC
            """, nativeQuery = true)
    List<CategoryProjection> findOrdersByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

}
