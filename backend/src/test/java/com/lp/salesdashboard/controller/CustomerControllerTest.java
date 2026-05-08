package com.lp.salesdashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lp.salesdashboard.dto.CustomerCreateRequest;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  CustomerService service;

    private static Customer customerEntity() {
        Customer c = new Customer();
        c.setId(1L);
        c.setName("Alice");
        c.setEmail("alice@test.com");
        c.setCity("Paris");
        return c;
    }

    // 

    @Test
    void createCustomer_withValidBody_returns201() throws Exception {
        given(service.createCustomer(any())).willReturn(customerEntity());

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CustomerCreateRequest("Alice", "alice@test.com", "Paris"))))
                .andExpect(status().isCreated());
    }

    // 

    @Test
    void createCustomer_withBlankName_returns400() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CustomerCreateRequest("", "alice@test.com", "Paris"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_withNullName_returns400() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":null,\"email\":\"alice@test.com\",\"city\":\"Paris\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_withInvalidEmail_returns400() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CustomerCreateRequest("Alice", "not-an-email", "Paris"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_withBlankCity_returns400() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CustomerCreateRequest("Alice", "alice@test.com", "  "))))
                .andExpect(status().isBadRequest());
    }

    // 

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }
}
