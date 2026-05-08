package com.lp.salesdashboard.repository;

import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.OrderItem;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.Product;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.projection.CategoryProjection;
import com.lp.salesdashboard.projection.TopProductProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class OrderItemRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrderItemRepository repo;

    private static final LocalDate JAN_01 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);

    private Customer customer;
    private Product laptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        customer = customer("Test Customer", "test@example.com", "Paris");

        laptop = product("Laptop", "Electronics", "999.99");
        phone  = product("Phone",  "Electronics", "499.99");
        Product book = product("Novel", "Books", "19.99");

        SalesOrder order1 = order(LocalDate.of(2026, 1, 15));
        SalesOrder order2 = order(LocalDate.of(2026, 1, 20));
        SalesOrder order3 = order(LocalDate.of(2026, 2, 5));   // outside range

        item(order1, laptop, 2, "999.99");  // 1999.98
        item(order1, book,   1, "19.99");   // 19.99
        item(order2, phone,  1, "499.99");  // 499.99
        item(order3, laptop, 1, "999.99");  // should be excluded from Jan queries

        em.flush();
    }

    @Test
    void findTopProducts_ranksProductsByRevenueDescending() {
        List<TopProductProjection> rows = repo.findTopProducts(JAN_01, JAN_31, PageRequest.of(0, 10));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getName()).isEqualTo("Laptop");   // highest revenue
        assertThat(rows.get(1).getName()).isEqualTo("Phone");
    }

    @Test
    void findTopProducts_respectsPageableLimit() {
        List<TopProductProjection> rows = repo.findTopProducts(JAN_01, JAN_31, PageRequest.of(0, 1));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void findTopProducts_excludesOrdersOutsideRange() {
        // Feb order has one laptop - in the full range it should NOT inflate Jan totals
        List<TopProductProjection> rows = repo.findTopProducts(JAN_01, JAN_31, PageRequest.of(0, 10));

        assertThat(rows.get(0).getRevenue()).isEqualByComparingTo("1999.98");
    }

    @Test
    void findOrdersByCategory_groupsCorrectly() {
        List<CategoryProjection> rows = repo.findOrdersByCategory(JAN_01, JAN_31);

        assertThat(rows).hasSize(2); // Electronics, Books
        CategoryProjection electronics = rows.get(0);
        assertThat(electronics.getCategory()).isEqualTo("Electronics");
        assertThat(electronics.getItemCount()).isEqualTo(3L); // laptop*2 + phone*1
    }

    // -------------------------------------------------------------------------

    private Customer customer(String name, String email, String city) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setCity(city);
        return em.persist(c);
    }

    private Product product(String name, String category, String price) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(new BigDecimal(price));
        return em.persist(p);
    }

    private SalesOrder order(LocalDate date) {
        SalesOrder o = new SalesOrder();
        o.setOrderDate(date);
        o.setTotalAmount(BigDecimal.ZERO);
        o.setCustomer(customer);
        o.setStatus(OrderStatus.DELIVERED);
        return em.persist(o);
    }

    private void item(SalesOrder order, Product product, int qty, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(unitPrice));
        em.persist(item);
    }
}
