package com.lp.salesdashboard.specification;

import com.lp.salesdashboard.entity.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Factory for JPA {@link Specification} predicates used to filter {@link SalesOrder} queries. */
public class OrderSpecifications {

    public static Specification<SalesOrder> betweenDates(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;
        if (from == null) return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("orderDate"), to);
        if (to == null)   return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("orderDate"), from);
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

    /** Range filter on {@code totalAmount}. Returns {@code null} when both bounds are null. */
    public static Specification<SalesOrder> amountBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            if (min != null && max != null) return cb.between(root.get("totalAmount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
            return cb.lessThanOrEqualTo(root.get("totalAmount"), max);
        };
    }

    /**
     * Correlated EXISTS subquery: keeps orders that contain at least one item
     * whose product category is in the given list (case-insensitive).
     * Uses a subquery instead of a direct join to stay compatible with the
     * {@code @EntityGraph} fetch join on {@code customer} and avoid duplicates in pagination.
     */
    public static Specification<SalesOrder> categoryIn(List<String> categories) {
        if (categories == null || categories.isEmpty()) return null;
        List<String> lower = categories.stream().map(String::toLowerCase).toList();
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<OrderItem> item = sub.from(OrderItem.class);
            Join<OrderItem, Product> product = item.join("product", JoinType.INNER);
            CriteriaBuilder.In<String> inClause = cb.in(cb.lower(product.get("category")));
            lower.forEach(inClause::value);
            sub.select(item.get("order").get("id"))
               .where(cb.and(
                   cb.equal(item.get("order").get("id"), root.get("id")),
                   inClause
               ));
            return cb.exists(sub);
        };
    }

    /** Restricts results to seed orders (creatorIp IS NULL) and orders created by the given IP. */
    public static Specification<SalesOrder> visibleToIp(String ip) {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("creatorIp")),
            cb.equal(root.get("creatorIp"), ip)
        );
    }

    /**
     * Correlated EXISTS subquery: keeps orders that contain at least one item
     * whose product name matches the given pattern (case-insensitive LIKE).
     */
    public static Specification<SalesOrder> productContains(String productName) {
        if (productName == null || productName.isBlank()) return null;
        String pattern = "%" + productName.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<OrderItem> item = sub.from(OrderItem.class);
            Join<OrderItem, Product> product = item.join("product", JoinType.INNER);
            sub.select(item.get("order").get("id"))
               .where(cb.and(
                   cb.equal(item.get("order").get("id"), root.get("id")),
                   cb.like(cb.lower(product.get("name")), pattern)
               ));
            return cb.exists(sub);
        };
    }
}
