package com.lp.salesdashboard.controller;

import com.lp.salesdashboard.dto.*;
import com.lp.salesdashboard.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StatsService statsService;

    private static final String FROM = "2026-01-01";
    private static final String TO   = "2026-01-31";

    @Test
    void getKpis_returns200WithKpiJson() throws Exception {
        given(statsService.getKpis(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(new KpiMetricsDto(
                        new BigDecimal("1500.00"), 10L,
                        new BigDecimal("150.00"), 5.0, 3.0, 2.0));

        mvc.perform(get("/api/stats/kpis").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(10))
                .andExpect(jsonPath("$.revenue").value(1500.00));
    }

    @Test
    void getKpis_missingParam_returns400() throws Exception {
        mvc.perform(get("/api/stats/kpis").param("from", FROM))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRevenueOverTime_returns200WithList() throws Exception {
        given(statsService.getRevenueOverTime(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(
                        new RevenuePointDto("2026-01-10", new BigDecimal("500.00")),
                        new RevenuePointDto("2026-01-20", new BigDecimal("200.00"))));

        mvc.perform(get("/api/stats/revenue-over-time").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("2026-01-10"))
                .andExpect(jsonPath("$[0].revenue").value(500.00));
    }

    @Test
    void getOrderCountOverTime_returns200WithList() throws Exception {
        given(statsService.getOrderCountOverTime(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(new RevenuePointDto("2026-01-10", BigDecimal.valueOf(3))));

        mvc.perform(get("/api/stats/orders-over-time").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].revenue").value(3));
    }

    @Test
    void getTopProducts_returns200WithList() throws Exception {
        given(statsService.getTopProducts(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(
                        new TopProductDto("Laptop", new BigDecimal("1999.98")),
                        new TopProductDto("Phone",  new BigDecimal("499.99"))));

        mvc.perform(get("/api/stats/top-products").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void getOrdersByCategory_returns200WithList() throws Exception {
        given(statsService.getOrdersByCategory(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(
                        new CategoryBreakdownDto("Electronics", 42L),
                        new CategoryBreakdownDto("Books", 7L)));

        mvc.perform(get("/api/stats/orders-by-category").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("Electronics"))
                .andExpect(jsonPath("$[0].itemCount").value(42));
    }
}
