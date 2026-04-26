package com.lp.salesdashboard.specification;

import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.SalesOrder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

/** Factory for JPA {@link Specification} predicates used to filter {@link SalesOrder} queries. */
public class OrderSpecifications {

    public static Specification<SalesOrder> betweenDates(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get("orderDate"), from, to);
    }

    /**
     * Case-insensitive LIKE filter on the associated customer's name.
     * Returns {@code null} (no restriction) when the value is blank.
     * LIKE special characters ({@code %}, {@code _}, {@code \}) are escaped before pattern construction.
     */
    public static Specification<SalesOrder> customerContains(String customer) {
        if (customer == null || customer.isBlank()) return null;
        String pattern = "%" + customer.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            Join<SalesOrder, Customer> join = root.join("customer", JoinType.INNER);
            return cb.like(cb.lower(join.get("name")), pattern);
        };
    }

    /** IN filter on order status. Returns {@code null} (no restriction) when the list is null or empty. */
    public static Specification<SalesOrder> statusIn(List<OrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return (root, query, cb) -> {
            CriteriaBuilder.In<OrderStatus> inClause = cb.in(root.get("status"));
            statuses.forEach(inClause::value);
            return inClause;
        };
    }
}
