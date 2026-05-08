package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.projection.CustomerSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByUserCreatedFalse();


    @Query(value = """
            SELECT c.id AS id, c.name AS name, c.email AS email, c.city AS city,
                   COUNT(o.id) AS orderCount,
                   COALESCE(SUM(o.totalAmount), 0) AS lifetimeValue
            FROM Customer c LEFT JOIN SalesOrder o ON o.customer = c
            WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
            GROUP BY c.id, c.name, c.email, c.city
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.id)
            FROM Customer c
            WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Page<CustomerSummaryProjection> findAllWithOrderStats(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT c.id AS id, c.name AS name, c.email AS email, c.city AS city,
                   COUNT(o.id) AS orderCount,
                   COALESCE(SUM(o.totalAmount), 0) AS lifetimeValue
            FROM Customer c LEFT JOIN SalesOrder o ON o.customer = c
            WHERE c.id = :id
            GROUP BY c.id, c.name, c.email, c.city
            """)
    Optional<CustomerSummaryProjection> findByIdWithOrderStats(@Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Customer c WHERE c.userCreated = true")
    int deleteUserCreated();
}
