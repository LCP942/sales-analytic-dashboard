package com.lp.salesdashboard.service;

import com.lp.salesdashboard.entity.*;
import com.lp.salesdashboard.repository.CustomerRepository;
import com.lp.salesdashboard.repository.ProductRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class DataMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(DataMaintenanceService.class);

    private static final String[] PAYMENT_METHODS  = {"Credit Card", "PayPal", "Bank Transfer", "Apple Pay"};
    private static final BigDecimal[] SHIPPING      = {BigDecimal.ZERO, new BigDecimal("4.99"), new BigDecimal("9.99"), new BigDecimal("14.99")};
    private static final int BATCH_SIZE             = 500;

    @Value("${app.seeder.window-years:5}")
    private int windowYears;

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository   customerRepository;
    private final ProductRepository    productRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random        random  = new Random();

    public void ensureDataWindow() {
        if (!running.compareAndSet(false, true)) {
            log.debug("Data maintenance already running, skipping");
            return;
        }
        try {
            doEnsureDataWindow();
        } finally {
            running.set(false);
        }
    }

    private void doEnsureDataWindow() {
        LocalDate today     = LocalDate.now();
        LocalDate windowEnd = today.plusYears(windowYears);

        // Early exit — most hourly runs will stop here
        Optional<LocalDate> maxDate = salesOrderRepository.findMaxSystemOrderDate();
        if (maxDate.isPresent() && !maxDate.get().isBefore(windowEnd)) {
            return;
        }

        LocalDate windowStart   = today.minusYears(windowYears);
        LocalDate generateFrom  = maxDate.map(d -> d.plusDays(1)).orElse(windowStart);

        pruneOldOrders(windowStart);
        generateOrders(generateFrom, windowEnd);
    }

    private void pruneOldOrders(LocalDate windowStart) {
        int items  = salesOrderRepository.deleteOldSystemOrderItems(windowStart);
        int orders = salesOrderRepository.deleteOldSystemOrders(windowStart);
        if (orders > 0) {
            log.info("Pruned {} system orders ({} items) older than {}", orders, items, windowStart);
        }
    }

    private void generateOrders(LocalDate from, LocalDate to) {
        List<Customer> customers = customerRepository.findAllByUserCreatedFalse();
        List<Product>  products  = productRepository.findAllByUserCreatedFalse();

        if (customers.isEmpty() || products.isEmpty()) {
            log.warn("Cannot generate orders: system catalog is empty (customers={}, products={})",
                     customers.size(), products.size());
            return;
        }

        log.info("Generating orders from {} to {}...", from, to);
        LocalDate today = LocalDate.now();
        List<SalesOrder> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int count = pickDailyOrderCount();
            for (int i = 0; i < count; i++) {
                batch.add(buildOrder(date, today, customers, products));
            }
            if (batch.size() >= BATCH_SIZE) {
                salesOrderRepository.saveAll(batch);
                total += batch.size();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            salesOrderRepository.saveAll(batch);
            total += batch.size();
        }

        log.info("Generated {} orders from {} to {}", total, from, to);
    }

    private SalesOrder buildOrder(LocalDate date, LocalDate today,
                                  List<Customer> customers, List<Product> products) {
        SalesOrder order = new SalesOrder();
        order.setOrderDate(date);
        order.setCustomer(customers.get(random.nextInt(customers.size())));
        order.setStatus(pickStatus(date, today));
        order.setPaymentMethod(PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)]);
        BigDecimal shipping = SHIPPING[random.nextInt(SHIPPING.length)];
        order.setShippingAmount(shipping);
        order.setUserCreated(false);

        addItem(order, products);
        if (random.nextDouble() < 0.6) addItem(order, products);

        BigDecimal total = order.getItems().stream()
            .map(oi -> oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(shipping);
        order.setTotalAmount(total);

        return order;
    }

    private void addItem(SalesOrder order, List<Product> products) {
        Product product = products.get(random.nextInt(products.size()));
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1 + random.nextInt(3));
        item.setUnitPrice(product.getPrice());
        order.getItems().add(item);
    }

    private int pickDailyOrderCount() {
        // 10% → 0 orders, 40% → 1, 40% → 2, 10% → 3  (avg 1.5/day)
        double r = random.nextDouble();
        if (r < 0.10) return 0;
        if (r < 0.50) return 1;
        if (r < 0.90) return 2;
        return 3;
    }

    private OrderStatus pickStatus(LocalDate date, LocalDate today) {
        if (date.isAfter(today)) {
            return random.nextBoolean() ? OrderStatus.PENDING : OrderStatus.CONFIRMED;
        }
        long daysAgo = ChronoUnit.DAYS.between(date, today);
        double r = random.nextDouble();
        if (daysAgo <= 7) {
            if (r < 0.40) return OrderStatus.PENDING;
            if (r < 0.70) return OrderStatus.CONFIRMED;
            if (r < 0.90) return OrderStatus.SHIPPED;
            return OrderStatus.CANCELLED;
        }
        if (daysAgo <= 30) {
            if (r < 0.10) return OrderStatus.PENDING;
            if (r < 0.25) return OrderStatus.CONFIRMED;
            if (r < 0.50) return OrderStatus.SHIPPED;
            if (r < 0.85) return OrderStatus.DELIVERED;
            return OrderStatus.CANCELLED;
        }
        if (r < 0.10) return OrderStatus.PENDING;
        if (r < 0.25) return OrderStatus.CONFIRMED;
        if (r < 0.45) return OrderStatus.SHIPPED;
        if (r < 0.90) return OrderStatus.DELIVERED;
        return OrderStatus.CANCELLED;
    }
}
