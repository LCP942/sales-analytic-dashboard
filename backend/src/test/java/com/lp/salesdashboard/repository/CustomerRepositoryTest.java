package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.projection.CustomerSummaryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CustomerRepositoryTest {

    @Autowired TestEntityManager   em;
    @Autowired CustomerRepository  repo;

    private static final String IP_A = "1.2.3.4";
    private static final String IP_B = "5.6.7.8";

    private Customer seedCustomer;
    private Customer aliceCustomer;
    private Customer bobCustomer;

    @BeforeEach
    void setUp() {
        seedCustomer  = em.persist(customer("Seed",  null));
        aliceCustomer = em.persist(customer("Alice", IP_A));
        bobCustomer   = em.persist(customer("Bob",   IP_B));
        em.flush();
    }

    @Test
    void findAllWithOrderStats_returnsSeedAndOwnCustomers() {
        var page = repo.findAllWithOrderStats(null, IP_A, Pageable.unpaged());

        assertThat(page.getContent())
                .extracting(CustomerSummaryProjection::getName)
                .containsExactlyInAnyOrder("Seed", "Alice")
                .doesNotContain("Bob");
    }

    @Test
    void findAllWithOrderStats_excludesOtherIpCustomers() {
        var page = repo.findAllWithOrderStats(null, IP_A, Pageable.unpaged());

        assertThat(page.getContent())
                .extracting(CustomerSummaryProjection::getName)
                .doesNotContain("Bob");
    }

    @Test
    void findByIdWithOrderStats_returnsEmpty_whenCustomerBelongsToOtherIp() {
        var result = repo.findByIdWithOrderStats(bobCustomer.getId(), IP_A);

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdWithOrderStats_returnsSeedCustomer_forAnyIp() {
        var result = repo.findByIdWithOrderStats(seedCustomer.getId(), IP_B);

        assertThat(result).isPresent()
                .get().extracting(CustomerSummaryProjection::getName)
                .isEqualTo("Seed");
    }

    private static Customer customer(String name, String creatorIp) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(name.toLowerCase() + "@test.com");
        c.setCity("Paris");
        c.setCreatorIp(creatorIp);
        return c;
    }
}
