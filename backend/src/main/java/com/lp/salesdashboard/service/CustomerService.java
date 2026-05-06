package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.CustomerCreateRequest;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.projection.CustomerSummaryProjection;
import com.lp.salesdashboard.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Page<CustomerSummaryProjection> getCustomers(String name, Pageable pageable) {
        String search = (name == null || name.isBlank()) ? null : name;
        return customerRepository.findAllWithOrderStats(search, pageable);
    }

    @Transactional(readOnly = true)
    public CustomerSummaryProjection getCustomer(Long id) {
        return customerRepository.findByIdWithOrderStats(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));
    }

    @Transactional
    public Customer createCustomer(CustomerCreateRequest req) {
        var c = new Customer();
        c.setName(req.name().trim());
        c.setEmail(req.email().trim());
        c.setCity(req.city().trim());
        c.setUserCreated(true);
        return customerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerById(Long id) {
        return customerRepository.findById(id);
    }
}
