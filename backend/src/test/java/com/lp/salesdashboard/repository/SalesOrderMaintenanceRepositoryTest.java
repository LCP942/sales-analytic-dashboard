package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class SalesOrderMaintenanceRepositoryTest {

    @Autowired TestEntityManager    em;
    @Autowired SalesOrderRepository repo;

    private Customer systemCustomer;
    private Customer userCustomer;
    private Product  product;

    @BeforeEach
    void setUp() {
        systemCustomer = em.persist(customer("System", false));
        userCustomer   = em.persist(customer("User",   true));
        product        = em.persist(product());
        em.flush();
    }

    // 

    @Test
    void findMaxSystemOrderDate_returnsLatestSystemOrder() {
        persistOrder(LocalDate.of(2022, 1, 1), false, systemCustomer);
        persistOrder(LocalDate.of(2025, 6, 1), false, systemCustomer); // latest system
        em.flush();

        assertThat(repo.findMaxSystemOrderDate())
            .contains(LocalDate.of(2025, 6, 1));
    }

    @Test
    void findMaxSystemOrderDate_ignoresUserCreatedOrders() {
        persistOrder(LocalDate.of(2024, 1, 1), false, systemCustomer); // system
        persistOrder(LocalDate.of(2030, 1, 1), true,  userCustomer);   // user, later date
        em.flush();

        assertThat(repo.findMaxSystemOrderDate())
            .contains(LocalDate.of(2024, 1, 1));
    }

    @Test
    void findMaxSystemOrderDate_returnsEmpty_whenNoSystemOrders() {
        persistOrder(LocalDate.of(2030, 1, 1), true, userCustomer);
        em.flush();

        assertThat(repo.findMaxSystemOrderDate()).isEmpty();
    }

    @Test
    void findMaxSystemOrderDate_returnsEmpty_whenNoOrdersAtAll() {
        assertThat(repo.findMaxSystemOrderDate()).isEqualTo(Optional.empty());
    }

    // 

    @Test
    void deleteOldSystemOrders_removesSystemOrdersBeforeCutoff() {
        persistOrder(LocalDate.of(2020, 1, 1), false, systemCustomer); // old system -> deleted
        persistOrder(LocalDate.of(2025, 1, 1), false, systemCustomer); // recent system -> kept
        em.flush();

        repo.deleteOldSystemOrderItems(LocalDate.of(2023, 1, 1));
        repo.deleteOldSystemOrders(LocalDate.of(2023, 1, 1));
        em.clear();

        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void deleteOldSystemOrders_doesNotRemoveUserCreatedOrders() {
        persistOrder(LocalDate.of(2020, 1, 1), true, userCustomer); // old but user-created -> kept
        em.flush();

        repo.deleteOldSystemOrderItems(LocalDate.of(2023, 1, 1));
        repo.deleteOldSystemOrders(LocalDate.of(2023, 1, 1));
        em.clear();

        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void deleteOldSystemOrders_doesNotRemoveOrdersOnCutoffDate() {
        LocalDate cutoff = LocalDate.of(2023, 1, 1);
        persistOrder(cutoff, false, systemCustomer); // exactly on cutoff boundary -> kept
        em.flush();

        repo.deleteOldSystemOrderItems(cutoff);
        repo.deleteOldSystemOrders(cutoff);
        em.clear();

        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void deleteOldSystemOrders_alsoDeletesTheirItems() {
        var order = persistOrderWithItem(LocalDate.of(2020, 1, 1), false, systemCustomer);
        em.flush();

        assertThat(em.find(SalesOrder.class, order.getId())).isNotNull();

        repo.deleteOldSystemOrderItems(LocalDate.of(2023, 1, 1));
        repo.deleteOldSystemOrders(LocalDate.of(2023, 1, 1));
        em.clear();

        assertThat(repo.count()).isEqualTo(0);
    }

    // 

    private void persistOrder(LocalDate date, boolean userCreated, Customer c) {
        SalesOrder o = new SalesOrder();
        o.setOrderDate(date);
        o.setTotalAmount(new BigDecimal("100.00"));
        o.setCustomer(c);
        o.setStatus(OrderStatus.DELIVERED);
        o.setUserCreated(userCreated);
        em.persist(o);
    }

    private SalesOrder persistOrderWithItem(LocalDate date, boolean userCreated, Customer c) {
        SalesOrder o = new SalesOrder();
        o.setOrderDate(date);
        o.setTotalAmount(new BigDecimal("100.00"));
        o.setCustomer(c);
        o.setStatus(OrderStatus.DELIVERED);
        o.setUserCreated(userCreated);
        em.persist(o);

        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        em.persist(item);

        return o;
    }

    private static Customer customer(String name, boolean userCreated) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(name.toLowerCase() + "@test.com");
        c.setCity("Paris");
        c.setUserCreated(userCreated);
        return c;
    }

    private static Product product() {
        Product p = new Product();
        p.setName("Widget");
        p.setCategory("Electronics");
        p.setPrice(new BigDecimal("49.99"));
        p.setUserCreated(false);
        return p;
    }
}
