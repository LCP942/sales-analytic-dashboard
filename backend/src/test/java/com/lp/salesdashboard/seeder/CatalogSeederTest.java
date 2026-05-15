package com.lp.salesdashboard.seeder;

import com.lp.salesdashboard.entity.Customer;
import com.lp.salesdashboard.entity.Product;
import com.lp.salesdashboard.repository.CustomerRepository;
import com.lp.salesdashboard.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogSeederTest {

    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository  productRepository;

    @InjectMocks CatalogSeeder seeder;

    @Captor ArgumentCaptor<List<Customer>> customerCaptor;
    @Captor ArgumentCaptor<List<Product>>  productCaptor;

    @Test
    void run_seeds20Customers_whenTableEmpty() throws Exception {
        given(customerRepository.count()).willReturn(0L);
        given(productRepository.count()).willReturn(1L);

        seeder.run(null);

        verify(customerRepository).saveAll(customerCaptor.capture());
        assertThat(customerCaptor.getValue()).hasSize(20);
    }

    @Test
    void run_seeds12Products_whenTableEmpty() throws Exception {
        given(customerRepository.count()).willReturn(1L);
        given(productRepository.count()).willReturn(0L);

        seeder.run(null);

        verify(productRepository).saveAll(productCaptor.capture());
        assertThat(productCaptor.getValue()).hasSize(12);
    }

    @Test
    void run_skipsCustomers_whenAlreadyPresent() throws Exception {
        given(customerRepository.count()).willReturn(20L);
        given(productRepository.count()).willReturn(0L);

        seeder.run(null);

        verify(customerRepository, never()).saveAll(customerCaptor.capture());
    }

    @Test
    void run_skipsProducts_whenAlreadyPresent() throws Exception {
        given(customerRepository.count()).willReturn(0L);
        given(productRepository.count()).willReturn(12L);

        seeder.run(null);

        verify(productRepository, never()).saveAll(productCaptor.capture());
    }

    @Test
    void allSeededCustomers_haveUserCreatedFalse() throws Exception {
        given(customerRepository.count()).willReturn(0L);
        given(productRepository.count()).willReturn(1L);

        seeder.run(null);

        verify(customerRepository).saveAll(customerCaptor.capture());
        assertThat(customerCaptor.getValue()).allMatch(c -> c.getCreatorIp() == null);
    }

    @Test
    void allSeededProducts_haveUserCreatedFalse() throws Exception {
        given(customerRepository.count()).willReturn(1L);
        given(productRepository.count()).willReturn(0L);

        seeder.run(null);

        verify(productRepository).saveAll(productCaptor.capture());
        assertThat(productCaptor.getValue()).allMatch(p -> !p.isUserCreated());
    }

    @Test
    void allSeededCustomers_haveNonBlankFields() throws Exception {
        given(customerRepository.count()).willReturn(0L);
        given(productRepository.count()).willReturn(1L);

        seeder.run(null);

        verify(customerRepository).saveAll(customerCaptor.capture());
        assertThat(customerCaptor.getValue()).allMatch(c ->
            c.getName() != null && !c.getName().isBlank() &&
            c.getEmail() != null && !c.getEmail().isBlank() &&
            c.getCity() != null && !c.getCity().isBlank());
    }

    @Test
    void allSeededProducts_havePositivePrice() throws Exception {
        given(customerRepository.count()).willReturn(1L);
        given(productRepository.count()).willReturn(0L);

        seeder.run(null);

        verify(productRepository).saveAll(productCaptor.capture());
        assertThat(productCaptor.getValue()).allMatch(p ->
            p.getPrice() != null && p.getPrice().signum() > 0);
    }
}
