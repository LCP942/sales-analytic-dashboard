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

public class OrderSpecifications {

    public static Specification<SalesOrder> betweenDates(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.between(root.get("orderDate"), from, to);
    }

    public static Specification<SalesOrder> customerContains(String customer) {
        if (customer == null || customer.isBlank()) return null;
        String pattern = "%" + customer.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            Join<SalesOrder, Customer> join = root.join("customer", JoinType.INNER);
            return cb.like(cb.lower(join.get("name")), pattern);
        };
    }

    public static Specification<SalesOrder> statusIn(List<OrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return (root, query, cb) -> {
            CriteriaBuilder.In<OrderStatus> inClause = cb.in(root.get("status"));
            statuses.forEach(inClause::value);
            return inClause;
        };
    }
}
