package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.CustomerCreateRequest;
import com.lp.salesdashboard.dto.CustomerDto;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.repository.CustomerRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import com.lp.salesdashboard.repository.h2.CustomerH2Dao;
import com.lp.salesdashboard.repository.h2.OrderH2Dao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages customers across two sources:
 * <ul>
 *   <li>MySQL (read-only) — existing seeded customers via JPA</li>
 *   <li>H2 in-memory — customers created through the API via JdbcTemplate</li>
 * </ul>
 * List and detail endpoints merge both sources transparently.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository    mysqlRepo;
    private final SalesOrderRepository  orderRepo;
    private final CustomerH2Dao         h2Customers;
    private final OrderH2Dao            h2Orders;

    /**
     * Returns a merged, paginated list of customers from both MySQL and H2.
     * H2 customers (newest) appear first, followed by MySQL customers.
     * Optional name filter applies to both sources.
     */
    @Transactional(readOnly = true)
    public Page<CustomerDto> getCustomers(String name, Pageable pageable) {
        // H2 customers — load all and filter in memory (small dataset)
        List<CustomerDto> h2All = h2Customers.findAll().stream()
                .filter(c -> name == null || name.isBlank()
                        || c.getName().toLowerCase().contains(name.toLowerCase()))
                .map(c -> toDto(c, h2Customers.countOrdersByCustomerId(c.getId()), BigDecimal.ZERO))
                .toList();

        // MySQL customers — paginated after accounting for H2 count
        int h2Count  = h2All.size();
        int pageNum  = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        // Virtual page: [h2All ... | mysqlAll ...]
        // Compute how many MySQL rows we need to fill this page
        int virtualStart = pageNum * pageSize;
        int virtualEnd   = virtualStart + pageSize;

        // Items from H2 that fall in the virtual window
        List<CustomerDto> h2InPage = h2All.subList(
                Math.min(virtualStart, h2Count),
                Math.min(virtualEnd, h2Count));

        // Remaining slots filled by MySQL
        int mysqlNeeded = pageSize - h2InPage.size();
        long mysqlOffset = Math.max(0L, virtualStart - h2Count);

        List<CustomerDto> mysqlPage = List.of();
        long mysqlTotal = 0;
        if (mysqlNeeded > 0) {
            Page<Customer> dbPage = name == null || name.isBlank()
                    ? mysqlRepo.findAll(pageable.withPage((int) (mysqlOffset / pageSize)))
                    : mysqlRepo.findByNameContainingIgnoreCase(name, pageable.withPage((int) (mysqlOffset / pageSize)));
            mysqlTotal = dbPage.getTotalElements();
            mysqlPage  = dbPage.getContent().stream()
                    .limit(mysqlNeeded)
                    .map(c -> {
                        int orderCount = (int) orderRepo.countByCustomerId(c.getId());
                        BigDecimal lv  = orderRepo.sumTotalByCustomerId(c.getId());
                        return toDto(c, orderCount, lv);
                    })
                    .toList();
        } else {
            // Still need the total for pagination
            mysqlTotal = name == null || name.isBlank()
                    ? mysqlRepo.count()
                    : mysqlRepo.findByNameContainingIgnoreCase(name, Pageable.unpaged()).getTotalElements();
        }

        List<CustomerDto> content = new ArrayList<>(h2InPage);
        content.addAll(mysqlPage);

        return new PageImpl<>(content, pageable, h2Count + mysqlTotal);
    }

    /**
     * Returns the full detail of a customer by ID.
     * Tries H2 first (free in-memory lookup), then falls back to MySQL.
     */
    @Transactional(readOnly = true)
    public CustomerDto getCustomer(Long id) {
        Customer c = h2Customers.findById(id)
                .or(() -> mysqlRepo.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));

        long orderCount = h2Orders.countByCustomerId(id) + orderRepo.countByCustomerId(id);
        BigDecimal lv   = h2Orders.sumTotalByCustomerId(id).add(orderRepo.sumTotalByCustomerId(id));
        return toDto(c, (int) orderCount, lv);
    }

    /**
     * Creates a new customer in H2.
     * The customer is immediately visible in the merged list.
     */
    public CustomerDto createCustomer(CustomerCreateRequest req) {
        if (req.name() == null || req.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (req.email() == null || req.email().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        if (req.city() == null || req.city().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required");

        var c = new Customer();
        c.setName(req.name().trim());
        c.setEmail(req.email().trim());
        c.setCity(req.city().trim());
        c = h2Customers.save(c);
        return toDto(c, 0, BigDecimal.ZERO);
    }

    /** Resolves a customer by ID from either datasource; used by OrderService. */
    public Optional<Customer> findCustomerById(Long id) {
        return h2Customers.findById(id).or(() -> mysqlRepo.findById(id));
    }

    // -------------------------------------------------------------------------

    private static CustomerDto toDto(Customer c, long orderCount, BigDecimal lv) {
        return new CustomerDto(c.getId(), c.getName(), c.getEmail(), c.getCity(),
                (int) orderCount, lv);
    }
}
