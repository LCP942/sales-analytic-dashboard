package com.lp.salesdashboard.repository.h2;

import com.lp.salesdashboard.dto.OrderItemRequest;
import com.lp.salesdashboard.dto.OrderSummaryDto;
import com.lp.salesdashboard.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO for order records stored in the H2 in-memory datasource.
 * customer_id may reference either a MySQL customer or an H2 customer —
 * resolution is handled by the service layer.
 */
@Repository
@RequiredArgsConstructor
public class OrderH2Dao {

    @Qualifier("h2JdbcTemplate")
    private final JdbcTemplate jdbc;

    // Fetches order rows joined with customer name from H2 customers table.
    // Falls back to a placeholder when the customer lives in MySQL.
    private static final String SELECT_SUMMARY = """
            SELECT o.id, o.order_date, o.total_amount, o.customer_id, o.status,
                   COALESCE(c.name, CAST(o.customer_id AS VARCHAR)) AS customer_name
            FROM sales_orders o
            LEFT JOIN customers c ON c.id = o.customer_id
            """;

    private static final RowMapper<OrderSummaryDto> SUMMARY_MAPPER = (rs, rowNum) ->
            new OrderSummaryDto(
                    rs.getLong("id"),
                    rs.getDate("order_date").toLocalDate(),
                    rs.getString("customer_name"),
                    rs.getBigDecimal("total_amount"),
                    OrderStatus.valueOf(rs.getString("status")));

    public record OrderH2Row(
            Long id, LocalDate orderDate, BigDecimal totalAmount, Long customerId,
            OrderStatus status, String paymentMethod, BigDecimal shippingAmount) {}

    private static final RowMapper<OrderH2Row> ROW_MAPPER = (rs, rowNum) ->
            new OrderH2Row(
                    rs.getLong("id"),
                    rs.getDate("order_date").toLocalDate(),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("customer_id"),
                    OrderStatus.valueOf(rs.getString("status")),
                    rs.getString("payment_method"),
                    rs.getBigDecimal("shipping_amount"));

    /**
     * Saves an order to H2. Items are saved in a second pass after the order ID is generated.
     * Returns the generated order ID.
     */
    public Long saveOrder(LocalDate orderDate, BigDecimal totalAmount, Long customerId,
                          OrderStatus status, String paymentMethod, BigDecimal shippingAmount,
                          List<OrderItemRequest> items) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sales_orders (order_date, total_amount, customer_id, status, payment_method, shipping_amount) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setDate(1, Date.valueOf(orderDate));
            ps.setBigDecimal(2, totalAmount);
            ps.setLong(3, customerId);
            ps.setString(4, status.name());
            ps.setString(5, paymentMethod);
            ps.setBigDecimal(6, shippingAmount);
            return ps;
        }, keyHolder);

        long orderId = keyHolder.getKey().longValue();
        for (OrderItemRequest item : items) {
            jdbc.update(
                    "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?,?,?,?)",
                    orderId, item.productId(), item.quantity(), item.unitPrice());
        }
        return orderId;
    }

    public List<OrderSummaryDto> findAllSummaries() {
        return jdbc.query(SELECT_SUMMARY + " ORDER BY o.order_date DESC", SUMMARY_MAPPER);
    }

    public Optional<OrderH2Row> findById(Long id) {
        var results = jdbc.query(
                "SELECT id, order_date, total_amount, customer_id, status, payment_method, shipping_amount FROM sales_orders WHERE id = ?",
                ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /** Returns all order items for a given H2 order. */
    public record ItemRow(Long productId, int quantity, BigDecimal unitPrice) {}

    public List<ItemRow> findItemsByOrderId(Long orderId) {
        return jdbc.query(
                "SELECT product_id, quantity, unit_price FROM order_items WHERE order_id = ?",
                (rs, rowNum) -> new ItemRow(rs.getLong("product_id"), rs.getInt("quantity"), rs.getBigDecimal("unit_price")),
                orderId);
    }

    public long countByCustomerId(Long customerId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE customer_id = ?", Long.class, customerId);
        return n == null ? 0 : n;
    }

    public BigDecimal sumTotalByCustomerId(Long customerId) {
        BigDecimal n = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM sales_orders WHERE customer_id = ?",
                BigDecimal.class, customerId);
        return n == null ? BigDecimal.ZERO : n;
    }
}
