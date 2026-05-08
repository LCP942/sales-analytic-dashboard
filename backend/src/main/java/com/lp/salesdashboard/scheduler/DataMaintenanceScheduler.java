package com.lp.salesdashboard.scheduler;

import com.lp.salesdashboard.service.DataMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataMaintenanceScheduler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMaintenanceScheduler.class);

    private final DataMaintenanceService dataMaintenanceService;

    @Override
    public void run(ApplicationArguments args) {
        // Run in background so the app becomes available immediately while data is generated
        Thread.ofVirtual().name("data-init").start(() -> {
            log.info("Starting background data window initialization");
            dataMaintenanceService.ensureDataWindow();
        });
    }

    // fixedDelay: next run starts 1 hour after the previous one completes — no overlap
    // initialDelay: skip the first hour since run() already triggers it on startup
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 3_600_000)
    public void maintain() {
        dataMaintenanceService.ensureDataWindow();
    }
}
