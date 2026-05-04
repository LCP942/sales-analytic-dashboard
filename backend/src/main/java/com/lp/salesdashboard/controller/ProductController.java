package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.ProductDto;
import com.lp.salesdashboard.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repo;

    @GetMapping
    public List<ProductDto> getProducts() {
        return repo.findAll().stream()
                .map(p -> new ProductDto(p.getId(), p.getName(), p.getCategory(), p.getPrice()))
                .toList();
    }
}
