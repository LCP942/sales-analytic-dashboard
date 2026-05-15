package com.lp.salesdashboard.service;

import com.lp.salesdashboard.dto.OrderCreateRequest;
import com.lp.salesdashboard.dto.OrderItemRequest;
import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.OrderStatus;
import com.lp.salesdashboard.entity.Product;
import com.lp.salesdashboard.entity.SalesOrder;
import com.lp.salesdashboard.repository.ProductRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock SalesOrderRepository orderRepo;
    @Mock ProductRepository    productRepo;
    @Mock CustomerService      customerService;

    @InjectMocks OrderService service;

    @Test
    void createOrder_throwsBadRequest_whenCustomerNotFound() {
        given(customerService.findCustomerById(99L)).willReturn(Optional.empty());

        var req = new OrderCreateRequest(
                99L, LocalDate.now(), OrderStatus.PENDING, "Credit Card", BigDecimal.ZERO,
                List.of(new OrderItemRequest(1L, 1, BigDecimal.TEN)));

        assertThatThrownBy(() -> service.createOrder(req, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createOrder_setsCreatorIp_onSavedOrder() {
        Customer customer = new Customer();
        customer.setName("Alice");
        customer.setEmail("alice@test.com");
        customer.setCity("Paris");

        Product product = new Product();
        product.setName("Widget");
        product.setCategory("Electronics");

        given(customerService.findCustomerById(1L)).willReturn(Optional.of(customer));
        given(productRepo.findById(any())).willReturn(Optional.of(product));

        SalesOrder saved = new SalesOrder();
        saved.setOrderDate(LocalDate.now());
        saved.setTotalAmount(BigDecimal.TEN);
        saved.setShippingAmount(BigDecimal.ZERO);
        saved.setPaymentMethod("Credit Card");
        saved.setCustomer(customer);
        saved.setStatus(OrderStatus.PENDING);
        saved.setCreatorIp("127.0.0.1");
        saved.getItems().addAll(new ArrayList<>());
        given(orderRepo.save(any(SalesOrder.class))).willReturn(saved);
        given(orderRepo.findWithItemsById(any())).willReturn(Optional.of(saved));

        var req = new OrderCreateRequest(
                1L, LocalDate.now(), OrderStatus.PENDING, "Credit Card", BigDecimal.ZERO,
                List.of(new OrderItemRequest(1L, 1, BigDecimal.TEN)));

        service.createOrder(req, "127.0.0.1");

        verify(orderRepo).save(argThat(o -> o.getCreatorIp() != null));
    }
}
