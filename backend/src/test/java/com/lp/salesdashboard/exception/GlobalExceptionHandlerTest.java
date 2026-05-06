package com.lp.salesdashboard.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lp.salesdashboard.controller.CustomerController;
import com.lp.salesdashboard.service.CustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;
    @MockBean  CustomerService customerService;

    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void attachAppender() {
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(logs);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(logs);
    }

    // -------------------------------------------------------------------------
    // ResponseStatusException → WARN
    // -------------------------------------------------------------------------

    @Test
    void notFound_logsWarnAndReturns404() throws Exception {
        given(customerService.getCustomer(99L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: 99"));

        mvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Customer not found: 99"));

        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .contains("404")
                .contains("Customer not found: 99")
                .contains("/api/customers/99");
    }

    // -------------------------------------------------------------------------
    // MethodArgumentNotValidException → WARN + champs invalides dans le corps
    // -------------------------------------------------------------------------

    @Test
    void validationError_logsWarnAndReturns400WithFieldDetails() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"alice@test.com\",\"city\":\"Paris\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields").value(org.hamcrest.Matchers.containsString("name")));

        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .contains("400")
                .contains("/api/customers");
    }

    // -------------------------------------------------------------------------
    // Exception inattendue → ERROR + corps générique sans détails internes
    // -------------------------------------------------------------------------

    @Test
    void unexpectedException_logsErrorAndReturns500WithGenericMessage() throws Exception {
        given(customerService.getCustomers(anyString(), any(Pageable.class)))
                .willThrow(new RuntimeException("Database connection lost"));

        mvc.perform(get("/api/customers"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"));

        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains("GET")
                .contains("/api/customers");
        assertThat(event.getThrowableProxy().getMessage())
                .isEqualTo("Database connection lost");
    }
}
