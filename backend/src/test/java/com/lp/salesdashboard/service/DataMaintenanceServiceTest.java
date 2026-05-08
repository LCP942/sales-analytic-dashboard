package com.lp.salesdashboard.service;

import com.lp.salesdashboard.entity.*;
import com.lp.salesdashboard.repository.CustomerRepository;
import com.lp.salesdashboard.repository.ProductRepository;
import com.lp.salesdashboard.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataMaintenanceServiceTest {

    @Mock SalesOrderRepository salesOrderRepository;
    @Mock CustomerRepository   customerRepository;
    @Mock ProductRepository    productRepository;

    @InjectMocks DataMaintenanceService service;

    @Captor ArgumentCaptor<List<SalesOrder>> batchCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "windowYears", 1);
    }

    // ── early-exit ────────────────────────────────────────────────────────────

    @Test
    void ensureDataWindow_exitsEarly_whenDataAlreadyCoversWindow() {
        given(salesOrderRepository.findMaxSystemOrderDate())
            .willReturn(Optional.of(LocalDate.now().plusYears(1)));

        service.ensureDataWindow();

        verify(salesOrderRepository, never()).deleteOldSystemOrderItems(any());
        verify(salesOrderRepository, never()).deleteOldSystemOrders(any());
        verify(salesOrderRepository, never()).saveAll(any());
    }

    @Test
    void ensureDataWindow_isNoOp_whenAlreadyRunning() {
        ReflectionTestUtils.setField(service, "running", new AtomicBoolean(true));

        service.ensureDataWindow();

        verify(salesOrderRepository, never()).findMaxSystemOrderDate();
    }

    // ── pruning ───────────────────────────────────────────────────────────────

    @Test
    void ensureDataWindow_prunesWithCorrectCutoffDate() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        LocalDate expectedCutoff = LocalDate.now().minusYears(1);
        verify(salesOrderRepository).deleteOldSystemOrderItems(expectedCutoff);
        verify(salesOrderRepository).deleteOldSystemOrders(expectedCutoff);
    }

    @Test
    void ensureDataWindow_prunesBeforeGenerating() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        var ordered = inOrder(salesOrderRepository);
        service.ensureDataWindow();

        ordered.verify(salesOrderRepository).deleteOldSystemOrderItems(any());
        ordered.verify(salesOrderRepository).deleteOldSystemOrders(any());
        ordered.verify(salesOrderRepository, atLeastOnce()).saveAll(any());
    }

    // ── generation triggers ───────────────────────────────────────────────────

    @Test
    void ensureDataWindow_generatesOrders_whenMaxDateBeforeWindowEnd() {
        given(salesOrderRepository.findMaxSystemOrderDate())
            .willReturn(Optional.of(LocalDate.now().plusMonths(6)));
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    void ensureDataWindow_doesNotGenerate_whenCatalogIsEmpty() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of());
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of());

        service.ensureDataWindow();

        verify(salesOrderRepository, never()).saveAll(any());
    }

    @Test
    void ensureDataWindow_generatesFromWindowStart_whenNoDataExists() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        LocalDate expectedWindowStart = LocalDate.now().minusYears(1);
        LocalDate earliest = allCaptured().stream()
            .map(SalesOrder::getOrderDate)
            .min(LocalDate::compareTo)
            .orElseThrow();
        assertThat(earliest).isAfterOrEqualTo(expectedWindowStart);
    }

    @Test
    void ensureDataWindow_generatesUpToWindowEnd() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        LocalDate expectedWindowEnd = LocalDate.now().plusYears(1);
        LocalDate latest = allCaptured().stream()
            .map(SalesOrder::getOrderDate)
            .max(LocalDate::compareTo)
            .orElseThrow();
        assertThat(latest).isBeforeOrEqualTo(expectedWindowEnd);
    }

    // ── generated order invariants ────────────────────────────────────────────

    @Test
    void generatedOrders_areNotUserCreated() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        assertThat(allCaptured()).allMatch(o -> !o.isUserCreated());
    }

    @Test
    void generatedOrders_eachHaveAtLeastOneItem() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        assertThat(allCaptured()).allMatch(o -> !o.getItems().isEmpty());
    }

    @Test
    void generatedOrders_totalAmountEqualsItemsTotalPlusShipping() {
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        for (SalesOrder order : allCaptured()) {
            BigDecimal expected = order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(order.getShippingAmount());
            assertThat(order.getTotalAmount())
                .as("order on %s", order.getOrderDate())
                .isEqualByComparingTo(expected);
        }
    }

    @Test
    void generatedOrders_referencesOnlySystemCustomers() {
        Customer system = systemCustomer();
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(system));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        assertThat(allCaptured()).allMatch(o -> !o.getCustomer().isUserCreated());
    }

    // ── status rules ──────────────────────────────────────────────────────────

    @Test
    void futureOrders_haveOnlyPendingOrConfirmedStatus() {
        // maxDate = today → only future orders are generated
        given(salesOrderRepository.findMaxSystemOrderDate())
            .willReturn(Optional.of(LocalDate.now()));
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        List<SalesOrder> futureOrders = allCaptured().stream()
            .filter(o -> o.getOrderDate().isAfter(LocalDate.now()))
            .toList();
        assertThat(futureOrders).isNotEmpty();
        assertThat(futureOrders).allMatch(o ->
            o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED);
    }

    @Test
    void recentOrders_doNotHaveDeliveredStatus() {
        // Generate full window from scratch so we get a large sample of dates
        given(salesOrderRepository.findMaxSystemOrderDate()).willReturn(Optional.empty());
        given(customerRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemCustomer()));
        given(productRepository.findAllByUserCreatedFalse()).willReturn(List.of(systemProduct()));

        service.ensureDataWindow();

        verify(salesOrderRepository, atLeastOnce()).saveAll(batchCaptor.capture());
        // Filter for the "≤7 days old" bucket — across ~730 days of generation
        // we're virtually guaranteed to have orders here
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<SalesOrder> recentOrders = allCaptured().stream()
            .filter(o -> !o.getOrderDate().isBefore(sevenDaysAgo)
                      && !o.getOrderDate().isAfter(LocalDate.now()))
            .toList();
        // The invariant must hold for any orders in this bucket (vacuously true when empty,
        // which is astronomically rare over 730 generated days)
        assertThat(recentOrders).allMatch(o -> o.getStatus() != OrderStatus.DELIVERED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<SalesOrder> allCaptured() {
        return batchCaptor.getAllValues().stream()
            .flatMap(Collection::stream)
            .toList();
    }

    private static Customer systemCustomer() {
        Customer c = new Customer();
        c.setName("System Customer");
        c.setEmail("system@test.com");
        c.setCity("Paris");
        c.setUserCreated(false);
        return c;
    }

    private static Product systemProduct() {
        Product p = new Product();
        p.setName("Widget");
        p.setCategory("Electronics");
        p.setPrice(new BigDecimal("99.99"));
        p.setUserCreated(false);
        return p;
    }
}
