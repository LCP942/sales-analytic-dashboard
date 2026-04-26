package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.dto.KpiRawDto;
import com.lp.salesdashboard.dto.RevenuePointDto;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.projection.DailyOrderCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class SalesOrderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SalesOrderRepository repo;

    private static final LocalDate JAN_01 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);

    private Customer customer;

    @BeforeEach
    void setUp() {
        Customer c = new Customer();
        c.setName("Test Customer");
        c.setEmail("test@example.com");
        c.setCity("Paris");
        customer = em.persist(c);

        persist(LocalDate.of(2026, 1, 10), "100.00");
        persist(LocalDate.of(2026, 1, 20), "200.00");
        persist(LocalDate.of(2026, 2, 5),  "999.00"); // outside range
        em.flush();
    }

    @Test
    void findKpiMetrics_returnsCorrectAggregatesForRange() {
        KpiRawDto kpi = repo.findKpiMetrics(JAN_01, JAN_31);

        assertThat(kpi.orderCount()).isEqualTo(2L);
        assertThat(kpi.revenue()).isEqualByComparingTo("300.00");
        assertThat(kpi.avgOrderValue()).isEqualByComparingTo("150.00");
    }

    @Test
    void findKpiMetrics_returnsNullRevenueForEmptyRange() {
        KpiRawDto kpi = repo.findKpiMetrics(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertThat(kpi.orderCount()).isEqualTo(0L);
        assertThat(kpi.revenue()).isNull();
    }

    @Test
    void findDailyRevenue_groupsByDateAndExcludesOutOfRange() {
        List<RevenuePointDto> points = repo.findDailyRevenue(JAN_01, JAN_31);

        assertThat(points).hasSize(2);
        assertThat(points).extracting(RevenuePointDto::revenue)
                .containsExactlyInAnyOrder(new BigDecimal("100.00"), new BigDecimal("200.00"));
    }

    @Test
    void findDailyRevenue_aggregatesMultipleOrdersOnSameDay() {
        persist(LocalDate.of(2026, 1, 10), "50.00");
        em.flush();

        List<RevenuePointDto> points = repo.findDailyRevenue(JAN_01, JAN_31);

        RevenuePointDto jan10 = points.stream()
                .filter(p -> p.label().contains("2026-01-10"))
                .findFirst()
                .orElseThrow();
        assertThat(jan10.revenue()).isEqualByComparingTo("150.00");
    }

    @Test
    void findDailyOrderCount_returnsCountPerDay() {
        List<DailyOrderCountProjection> rows = repo.findDailyOrderCount(JAN_01, JAN_31);

        assertThat(rows).hasSize(2);
    }

    // -------------------------------------------------------------------------

    private void persist(LocalDate date, String amount) {
        SalesOrder order = new SalesOrder();
        order.setOrderDate(date);
        order.setTotalAmount(new BigDecimal(amount));
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DELIVERED);
        em.persist(order);
    }
}
