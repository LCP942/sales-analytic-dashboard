package com.lp.salesdashboard.scheduler;

import com.lp.salesdashboard.repository.CustomerRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository   customerRepository;

    @Scheduled(cron = "0 0 0,12 * * *")
    @Transactional
    public void cleanupUserData() {
        int items     = salesOrderRepository.deleteUserCreatedOrderItems();
        int orders    = salesOrderRepository.deleteUserCreatedOrders();
        int customers = customerRepository.deleteUserCreated();
        log.info("Cleanup: deleted {} items, {} orders, {} customers", items, orders, customers);
    }
}
