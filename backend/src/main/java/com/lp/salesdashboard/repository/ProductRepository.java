package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByUserCreatedFalse();
}
