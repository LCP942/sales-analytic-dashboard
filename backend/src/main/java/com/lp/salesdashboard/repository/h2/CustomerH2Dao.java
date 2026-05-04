package com.lp.salesdashboard.repository.h2;

import com.lp.salesdashboard.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * DAO for customer records stored in the H2 in-memory datasource.
 * Only customers created through the API live here; MySQL customers are read-only.
 */
@Repository
@RequiredArgsConstructor
public class CustomerH2Dao {

    @Qualifier("h2JdbcTemplate")
    private final JdbcTemplate jdbc;

    private static final RowMapper<Customer> ROW_MAPPER = (rs, rowNum) -> {
        var c = new Customer();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        c.setEmail(rs.getString("email"));
        c.setCity(rs.getString("city"));
        return c;
    };

    public Customer save(Customer customer) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO customers (name, email, city) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getCity());
            return ps;
        }, keyHolder);
        // Customer.id has @Setter(AccessLevel.NONE) — re-fetch the inserted row instead of
        // trying to reflectively set the id field.
        return findById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public List<Customer> findAll() {
        return jdbc.query("SELECT id, name, email, city FROM customers ORDER BY id DESC", ROW_MAPPER);
    }

    public Optional<Customer> findById(Long id) {
        var results = jdbc.query(
                "SELECT id, name, email, city FROM customers WHERE id = ?",
                ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM customers", Long.class);
        return n == null ? 0 : n;
    }

    public long countOrdersByCustomerId(Long customerId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE customer_id = ?", Long.class, customerId);
        return n == null ? 0 : n;
    }
}
