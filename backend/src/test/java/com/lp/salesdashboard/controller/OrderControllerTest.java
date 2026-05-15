package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.entity.*;
import com.lp.salesdashboard.projection.CustomerSummaryProjection;
import com.lp.salesdashboard.service.CustomerService;
import com.lp.salesdashboard.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomerService customerService;

    private static final String FROM = "2026-01-01";
    private static final String TO   = "2026-01-31";

    private SalesOrder orderEntity() {
        try {
            Customer customer = new Customer();
            customer.setName("Alice Martin");
            customer.setEmail("alice@example.com");
            customer.setCity("Paris");
            setField(customer, "id", 10L);

            Product product = new Product();
            product.setName("Laptop");
            product.setCategory("Electronics");

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(1);
            item.setUnitPrice(new BigDecimal("249.99"));

            SalesOrder order = new SalesOrder();
            setField(order, "id", 1L);
            order.setCustomer(customer);
            order.setOrderDate(LocalDate.of(2026, 1, 15));
            order.setTotalAmount(new BigDecimal("299.99"));
            order.setShippingAmount(new BigDecimal("50.00"));
            order.setPaymentMethod("Credit Card");
            order.setStatus(OrderStatus.DELIVERED);
            item.setOrder(order);
            order.getItems().add(item);

            return order;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CustomerSummaryProjection customerStats() {
        CustomerSummaryProjection stats = mock(CustomerSummaryProjection.class);
        given(stats.getId()).willReturn(10L);
        given(stats.getName()).willReturn("Alice Martin");
        given(stats.getEmail()).willReturn("alice@example.com");
        given(stats.getCity()).willReturn("Paris");
        given(stats.getOrderCount()).willReturn(3L);
        given(stats.getLifetimeValue()).willReturn(new BigDecimal("899.97"));
        return stats;
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    // -------------------------------------------------------------------------
    // GET /api/orders
    // -------------------------------------------------------------------------

    @Test
    void getOrders_returns200WithPageContent() throws Exception {
        given(orderService.getOrders(
                any(), any(), anyString(), anyList(),
                any(), any(), anyList(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(orderEntity())));

        mvc.perform(get("/api/orders").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("DELIVERED"));
    }

    @Test
    void getOrders_withStatusFilter_passesStatusesToService() throws Exception {
        given(orderService.getOrders(
                any(), any(), anyString(),
                eq(List.of(OrderStatus.PENDING, OrderStatus.SHIPPED)),
                any(), any(), anyList(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(Page.empty());

        mvc.perform(get("/api/orders")
                        .param("from", FROM).param("to", TO)
                        .param("statuses", "PENDING,SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getOrders_invalidStatus_returns400() throws Exception {
        mvc.perform(get("/api/orders")
                        .param("from", FROM).param("to", TO)
                        .param("statuses", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrders_missingFrom_returns200() throws Exception {
        given(orderService.getOrders(
                isNull(), any(), anyString(), anyList(),
                any(), any(), anyList(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(Page.empty());

        mvc.perform(get("/api/orders").param("to", TO))
                .andExpect(status().isOk());
    }

    @Test
    void getOrders_missingTo_returns200() throws Exception {
        given(orderService.getOrders(
                any(), isNull(), anyString(), anyList(),
                any(), any(), anyList(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(Page.empty());

        mvc.perform(get("/api/orders").param("from", FROM))
                .andExpect(status().isOk());
    }

    @Test
    void getOrders_missingBothDates_returns200() throws Exception {
        given(orderService.getOrders(
                isNull(), isNull(), anyString(), anyList(),
                any(), any(), anyList(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(Page.empty());

        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /api/orders/{id}
    // -------------------------------------------------------------------------

    @Test
    void getOrder_returns200WithDetail() throws Exception {
        CustomerSummaryProjection stats = customerStats();
        given(orderService.getOrder(eq(1L), anyString())).willReturn(orderEntity());
        given(customerService.getCustomer(any(Long.class), anyString())).willReturn(stats);

        mvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.customer.name").value("Alice Martin"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.paymentMethod").value("Credit Card"));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        given(orderService.getOrder(eq(999L), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: 999"));

        mvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/orders
    // -------------------------------------------------------------------------

    @Test
    void createOrder_withNullCustomerId_returns400() throws Exception {
        String body = """
                {"customerId":null,"orderDate":"2026-01-01","status":"PENDING",
                 "paymentMethod":"Credit Card","shippingAmount":0,
                 "items":[{"productId":1,"quantity":1,"unitPrice":100.00}]}
                """;
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_withEmptyItems_returns400() throws Exception {
        String body = """
                {"customerId":1,"orderDate":"2026-01-01","status":"PENDING",
                 "paymentMethod":"Credit Card","shippingAmount":0,"items":[]}
                """;
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_withInvalidItemQuantity_returns400() throws Exception {
        String body = """
                {"customerId":1,"orderDate":"2026-01-01","status":"PENDING",
                 "paymentMethod":"Credit Card","shippingAmount":0,
                 "items":[{"productId":1,"quantity":0,"unitPrice":100.00}]}
                """;
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
