package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService orderService;

    private static final String FROM = "2026-01-01";
    private static final String TO   = "2026-01-31";

    private OrderSummaryDto summaryDto() {
        return new OrderSummaryDto(1L, LocalDate.of(2026, 1, 15),
                "Alice Martin", new BigDecimal("299.99"), OrderStatus.DELIVERED);
    }

    private OrderDetailDto detailDto() {
        CustomerDto customer = new CustomerDto(
                10L, "Alice Martin", "alice@example.com", "Paris", 3,
                new BigDecimal("899.97"));

        OrderItemDto item = new OrderItemDto(
                "Laptop", "Electronics", 1,
                new BigDecimal("249.99"), new BigDecimal("249.99"));

        return new OrderDetailDto(
                1L, LocalDate.of(2026, 1, 15),
                new BigDecimal("299.99"), OrderStatus.DELIVERED,
                customer, 1, List.of(item),
                new BigDecimal("249.99"), new BigDecimal("50.00"), "Credit Card");
    }

    // -------------------------------------------------------------------------
    // GET /api/orders
    // -------------------------------------------------------------------------

    @Test
    void getOrders_returns200WithPageContent() throws Exception {
        given(orderService.getOrders(
                any(), any(), anyString(), anyList(),
                any(), any(), anyList(), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(summaryDto())));

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
                any(), any(), anyList(), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

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
    void getOrders_missingFrom_returns400() throws Exception {
        mvc.perform(get("/api/orders").param("to", TO))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/orders/{id}
    // -------------------------------------------------------------------------

    @Test
    void getOrder_returns200WithDetail() throws Exception {
        given(orderService.getOrder(1L)).willReturn(detailDto());

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
        given(orderService.getOrder(999L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: 999"));

        mvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }
}
