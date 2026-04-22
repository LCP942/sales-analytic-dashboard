package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Top products by revenue within a date range.
     * Native query — returns [name, revenue] pairs, paginated via Pageable for the configurable limit.
     */
    @Query(value = """
            SELECT p.name, SUM(oi.quantity * oi.unit_price) AS revenue
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN sales_orders o ON oi.order_id = o.id
            WHERE o.order_date BETWEEN :from AND :to
            GROUP BY p.id, p.name
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> findTopProducts(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /**
     * Item count breakdown by product category for the doughnut chart.
     */
    @Query(value = """
            SELECT p.category, SUM(oi.quantity) AS item_count
            FROM order_items oi
            JOIN products p ON oi.product_id = p.id
            JOIN sales_orders o ON oi.order_id = o.id
            WHERE o.order_date BETWEEN :from AND :to
            GROUP BY p.category
            ORDER BY item_count DESC
            """, nativeQuery = true)
    List<Object[]> findOrdersByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
