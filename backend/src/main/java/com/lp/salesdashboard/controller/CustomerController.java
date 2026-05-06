package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.CustomerCreateRequest;
import com.lp.salesdashboard.dto.CustomerDto;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.projection.CustomerSummaryProjection;
import com.lp.salesdashboard.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Page<CustomerDto> getCustomers(
            @RequestParam(defaultValue = "") String name,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return customerService.getCustomers(name, pageable).map(CustomerController::toDto);
    }

    @GetMapping("/{id}")
    public CustomerDto getCustomer(@PathVariable Long id) {
        return toDto(customerService.getCustomer(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto createCustomer(@Valid @RequestBody CustomerCreateRequest req) {
        Customer c = customerService.createCustomer(req);
        return new CustomerDto(c.getId(), c.getName(), c.getEmail(), c.getCity(), 0, BigDecimal.ZERO);
    }

    private static CustomerDto toDto(CustomerSummaryProjection p) {
        return new CustomerDto(p.getId(), p.getName(), p.getEmail(), p.getCity(),
                p.getOrderCount().intValue(), p.getLifetimeValue());
    }
}
