package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** Case-insensitive name search, used for the customer list filter. */
    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
