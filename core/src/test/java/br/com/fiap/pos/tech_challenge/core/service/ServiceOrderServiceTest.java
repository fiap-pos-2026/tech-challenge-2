package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.OpenProductItemRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.domain.*;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.*;
import br.com.fiap.pos.tech_challenge.core.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.repository.*;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.service.event.ServiceOrderStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock ReworkCycleRepository reworkCycleRepository;
    @Mock MechanicalServiceRepository mechanicalServiceRepository;
    @Mock ProductRepository productRepository;
    @Mock CustomerService customerService;
    @Mock VehicleService vehicleService;
    @Mock StockService stockService;
    @Mock OTPService otpService;
    @Mock NotificationService notificationService;
    @Mock AuditLogService auditLogService;
    @Mock AuthenticationService authenticationService;
    @Mock UserRepository userRepository;
    @Mock ServiceOrderMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ServiceOrderService sut;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sut, "reworkHourlyRate", new BigDecimal("150.00"));
    }

    // -------------------------------------------------------------------------
    // openServiceOrder
    // -------------------------------------------------------------------------
    @Test
    void openServiceOrder_createsOrderWithStatusReceived() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrderResponse expected = responseFor(saved);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.openServiceOrder(customerUuid, vehicleUuid, "Barulho no motor");

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
    }

    @Test
    void openServiceOrder_withoutItems_returnsOrderUuidAndSkipsQuote() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        saved.setUuid(osUuid);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(new ServiceOrderResponse(
                osUuid, ServiceOrderStatus.RECEIVED, "Barulho no motor", null, LocalDateTime.now()));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.openServiceOrder(
                customerUuid, vehicleUuid, "Barulho no motor", List.of(), List.of());

        assertThat(result.uuid()).isEqualTo(osUuid);
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void openServiceOrder_withItems_persistsLinesInProvisionalQuote() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        MechanicalService ms = new MechanicalService();
        ms.setName("Troca de óleo");
        ms.setBasePrice(new BigDecimal("150.00"));
        ms.setEstimatedDurationMinutes(30);

        Product product = new Product();
        product.setName("Óleo 5W30");
        product.setUnitPrice(new BigDecimal("45.00"));

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        Quote quote = new Quote();

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(mechanicalServiceRepository.findByUuid(msUuid)).thenReturn(Optional.of(ms));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(saved)).thenReturn(responseFor(saved));
        when(mapper.toQuoteResponse(quote)).thenReturn(null);

        sut.openServiceOrder(customerUuid, vehicleUuid, "Revisão", List.of(msUuid),
                List.of(new OpenProductItemRequest(productUuid, new BigDecimal("2"))));

        assertThat(quote.getServiceLines()).hasSize(1);
        assertThat(quote.getServiceLines().get(0).getNameSnapshot()).isEqualTo("Troca de óleo");
        assertThat(quote.getProductLines()).hasSize(1);
        assertThat(quote.getProductLines().get(0).getNameSnapshot()).isEqualTo("Óleo 5W30");
        assertThat(quote.getProductLines().get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
        verify(quoteRepository).save(quote);
    }

    @Test
    void openServiceOrder_throwsNotFoundAndPersistsNothingWhenMechanicalServiceMissing() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(mechanicalServiceRepository.findByUuid(msUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.openServiceOrder(customerUuid, vehicleUuid, "Revisão",
                List.of(msUuid), List.of()))
                .isInstanceOf(MechanicalServiceNotFoundException.class);

        verify(serviceOrderRepository, never()).save(any());
    }

    @Test
    void openServiceOrder_throwsNotFoundAndPersistsNothingWhenProductMissing() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.openServiceOrder(customerUuid, vehicleUuid, "Revisão",
                List.of(), List.of(new OpenProductItemRequest(productUuid, BigDecimal.ONE))))
                .isInstanceOf(ProductNotFoundException.class);

        verify(serviceOrderRepository, never()).save(any());
    }

    @Test
    void openServiceOrder_publishesStatusEventWithNullPreviousStatus() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setEmail("cliente@mail.com");
        customer.setName("Cliente Teste");

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        saved.setUuid(osUuid);
        saved.setCustomer(customer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(responseFor(saved));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        sut.openServiceOrder(customerUuid, vehicleUuid, "Barulho no motor");

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.serviceOrderUuid()).isEqualTo(osUuid);
        assertThat(event.previousStatus()).isNull();
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(event.customerEmail()).isEqualTo("cliente@mail.com");
        assertThat(event.customerName()).isEqualTo("Cliente Teste");
    }

    // -------------------------------------------------------------------------
    // startDiagnosis
    // -------------------------------------------------------------------------
    @Test
    void startDiagnosis_transitionsFromReceivedToInDiagnosis() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrderResponse expected = new ServiceOrderResponse(uuid, ServiceOrderStatus.IN_DIAGNOSIS,
                "queixa", null, LocalDateTime.now());

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.startDiagnosis(uuid);

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
    }

    @Test
    void startDiagnosis_throwsWhenNotReceived() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.startDiagnosis(uuid))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // getServiceOrder
    // -------------------------------------------------------------------------
    @Test
    void getServiceOrder_returnsResponseWithQuote() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        Quote quote = new Quote();
        ServiceOrderResponse base = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(mapper.toResponse(so)).thenReturn(base);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(mapper.toQuoteResponse(quote)).thenReturn(null);

        sut.getServiceOrder(uuid);

        verify(quoteRepository, times(1)).findFirstByServiceOrderIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getServiceOrder_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getServiceOrder(uuid))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // listServiceOrders
    // -------------------------------------------------------------------------
    @Test
    void listServiceOrders_filtersByStatusWhenProvided() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        when(serviceOrderRepository.findWithFilters(
                eq(ServiceOrderStatus.RECEIVED), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(so)));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(ServiceOrderStatus.RECEIVED, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listServiceOrders_returnsAllWhenNoFilter() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        when(serviceOrderRepository.findWithFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(so)));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(null, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listServiceOrders_appliesExplicitCompletedFilterOverridingDefaultExclusion() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        when(serviceOrderRepository.findWithFilters(
                eq(ServiceOrderStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(so)));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(ServiceOrderStatus.COMPLETED, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).extracting(ServiceOrderResponse::status)
                .containsExactly(ServiceOrderStatus.COMPLETED);
        verify(serviceOrderRepository).findWithFilters(
                eq(ServiceOrderStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listServiceOrders_preservesBusinessPriorityOrderFromQuery() {
        ServiceOrder inProgress = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder awaiting = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        ServiceOrder inDiagnosis = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        ServiceOrder received = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrder cancelled = serviceOrderWithStatus(ServiceOrderStatus.CANCELLED);

        when(serviceOrderRepository.findWithFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inProgress, awaiting, inDiagnosis, received, cancelled)));
        when(mapper.toResponse(inProgress)).thenReturn(responseFor(inProgress));
        when(mapper.toResponse(awaiting)).thenReturn(responseFor(awaiting));
        when(mapper.toResponse(inDiagnosis)).thenReturn(responseFor(inDiagnosis));
        when(mapper.toResponse(received)).thenReturn(responseFor(received));
        when(mapper.toResponse(cancelled)).thenReturn(responseFor(cancelled));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(null, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).extracting(ServiceOrderResponse::status)
                .containsExactly(ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.AWAITING_APPROVAL,
                        ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.RECEIVED,
                        ServiceOrderStatus.CANCELLED);
    }

    @Test
    void listServiceOrders_dropsClientSortKeepingPageAndSize() {
        when(serviceOrderRepository.findWithFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        sut.listServiceOrders(null, null, null, null,
                PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "createdAt")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(serviceOrderRepository).findWithFilters(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(15);
    }

    // -------------------------------------------------------------------------
    // resendOTP
    // -------------------------------------------------------------------------
    @Test
    void resendOTP_invalidatesAndSendsNewToken() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        sut.resendOTP(uuid);

        verify(otpService).invalidateByServiceOrder(so);
        verify(otpService).generateAndSend(so);
    }

    @Test
    void resendOTP_succeedsWhenCompletedForDeliveryInspection() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        sut.resendOTP(uuid);

        verify(otpService).invalidateByServiceOrder(so);
        verify(otpService).generateAndSend(so);
    }

    @Test
    void resendOTP_throwsWhenNotAwaiting() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.resendOTP(uuid))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // completeExecution
    // -------------------------------------------------------------------------
    @Test
    void completeExecution_transitionsToCompleted() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrderResponse expected = new ServiceOrderResponse(uuid, ServiceOrderStatus.COMPLETED,
                "queixa", null, LocalDateTime.now());

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.completeExecution(uuid);

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.COMPLETED);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
    }

    @Test
    void completeExecution_throwsWhenNotInProgress() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.completeExecution(uuid))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // returnProduct — reauth check
    // -------------------------------------------------------------------------
    @Test
    void returnProduct_callsValidatePasswordAndCreditStock() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();
        String password = "s3cur3";

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine line = new QuoteProductLine();
        line.setProduct(product);
        line.setQuantity(BigDecimal.TWO);

        Quote quote = new Quote();
        quote.getProductLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getLogin()).thenReturn("atendente");
        when(principal.getId()).thenReturn(1L);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.returnProduct(osUuid, productUuid, password, principal);

        verify(authenticationService).validatePassword("atendente", password);
        verify(stockService).credit(eq(productUuid), eq(BigDecimal.TWO), eq(so), any());
    }

    // -------------------------------------------------------------------------
    // closeDispute — status guards
    // -------------------------------------------------------------------------
    @Test
    void closeDispute_throwsFromTerminalStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.DELIVERED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.closeDispute(uuid, "resolução", null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void closeDispute_succeedsFromCompletedStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.closeDispute(uuid, "resolução", null);

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DISPUTED);
        assertThat(result).isNotNull();

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.DISPUTED);
    }

    // -------------------------------------------------------------------------
    // openServiceOrder — vehicle ownership
    // -------------------------------------------------------------------------
    @Test
    void openServiceOrder_throwsWhenVehicleDoesNotBelongToCustomer() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Customer otherCustomer = new Customer();
        otherCustomer.setId(2L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(otherCustomer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);

        assertThatThrownBy(() -> sut.openServiceOrder(customerUuid, vehicleUuid, "Queixa"))
                .isInstanceOf(CoreException.class);
    }

    // -------------------------------------------------------------------------
    // startDiagnosis — terminal status guards
    // -------------------------------------------------------------------------
    @Test
    void startDiagnosis_throwsFromCancelledStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.CANCELLED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.startDiagnosis(uuid))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // returnProduct — stock service propagates ReturnNotAllowedException
    // -------------------------------------------------------------------------
    @Test
    void returnProduct_propagatesReturnNotAllowedException() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine line = new QuoteProductLine();
        line.setProduct(product);
        line.setQuantity(BigDecimal.ONE);

        Quote quote = new Quote();
        quote.getProductLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getLogin()).thenReturn("mecanico");
        when(principal.getId()).thenReturn(2L);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        doThrow(new ReturnNotAllowedException()).when(stockService)
                .credit(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.returnProduct(osUuid, productUuid, "pass", principal))
                .isInstanceOf(ReturnNotAllowedException.class);
    }

    // -------------------------------------------------------------------------
    // approveQuote — first approval debits all budgeted products
    // -------------------------------------------------------------------------
    @Test
    void approveQuote_debitsAllBudgetedProductsOnFirstApproval() {
        UUID uuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine budgeted = new QuoteProductLine();
        budgeted.setProduct(product);
        budgeted.setQuantity(new BigDecimal("2"));
        budgeted.setUnbudgeted(false);

        Quote quote = new Quote();
        quote.getProductLines().add(budgeted);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(expected);

        sut.approveQuote(uuid, "52998224725", "token");

        verify(stockService).debit(eq(productUuid), eq(new BigDecimal("2")), eq(so), isNull());

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    // -------------------------------------------------------------------------
    // approveQuote — addendum path clears unbudgeted flag, no extra debit
    // -------------------------------------------------------------------------
    @Test
    void approveQuote_clearsUnbudgetedLinesOnAddendum() {
        UUID uuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(UUID.randomUUID());

        QuoteProductLine unbudgeted = new QuoteProductLine();
        unbudgeted.setProduct(product);
        unbudgeted.setQuantity(BigDecimal.ONE);
        unbudgeted.setUnbudgeted(true);

        Quote quote = new Quote();
        quote.setApprovedAt(Instant.now().minusSeconds(3600)); // já foi aprovado antes
        quote.getProductLines().add(unbudgeted);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(expected);

        sut.approveQuote(uuid, "52998224725", "token");

        assertThat(unbudgeted.isUnbudgeted()).isFalse();
        verify(stockService, never()).debit(any(), any(), any(), any());
        verify(quoteRepository).save(quote);
    }

    // -------------------------------------------------------------------------
    // approveQuote — unbudgeted line present at first approval is NOT debited again
    // -------------------------------------------------------------------------
    @Test
    void approveQuote_doesNotDebitUnbudgetedLineOnFirstApproval() {
        UUID uuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine unbudgeted = new QuoteProductLine();
        unbudgeted.setProduct(product);
        unbudgeted.setQuantity(new BigDecimal("3"));
        unbudgeted.setUnbudgeted(true);

        Quote quote = new Quote();
        quote.getProductLines().add(unbudgeted);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(responseFor(so));

        sut.approveQuote(uuid, "52998224725", "token");

        verify(stockService, never()).debit(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // requestProduct — rejected during initial AWAITING_APPROVAL (quote not yet approved)
    // -------------------------------------------------------------------------
    @Test
    void requestProduct_throwsWhenCalledDuringInitialAwaitingApproval() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        Quote quote = new Quote(); // approvedAt == null → initial wait

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> sut.requestProduct(osUuid, productUuid, BigDecimal.ONE, null))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(stockService, never()).debit(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // rejectQuote — initial rejection cancels the order
    // -------------------------------------------------------------------------
    @Test
    void rejectQuote_cancelledWhenNoUnbudgetedLines() {
        UUID uuid = UUID.randomUUID();

        Quote quote = new Quote(); // empty product lines

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(responseFor(so));

        sut.rejectQuote(uuid, "52998224725", "token", null);

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
        verify(serviceOrderRepository).save(so);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
    }

    // -------------------------------------------------------------------------
    // rejectQuote — addendum rejection compensates stock and resumes IN_PROGRESS
    // -------------------------------------------------------------------------
    @Test
    void rejectQuote_compensatesStockAndResumesWhenAddendum() {
        UUID uuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine line = new QuoteProductLine();
        line.setId(1L); // non-null ID needed for equals-based removeAll
        line.setProduct(product);
        line.setQuantity(BigDecimal.TWO);
        line.setUnitPriceSnapshot(BigDecimal.TEN);
        line.setUnbudgeted(true);

        Quote quote = new Quote();
        quote.getProductLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getId()).thenReturn(3L);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(userRepository.findById(3L)).thenReturn(Optional.empty());
        when(mapper.toResponse(any())).thenReturn(responseFor(so));

        sut.rejectQuote(uuid, "52998224725", "token", principal);

        verify(stockService).compensate(eq(productUuid), eq(BigDecimal.TWO), eq(so), any());
        verify(quoteRepository).save(quote);
        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    // -----------------------------------------------------------------------
    // addServiceToDiagnosis
    // -----------------------------------------------------------------------
    @Test
    void addServiceToDiagnosis_addsServiceLineToQuote() {
        UUID osUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        MechanicalService ms = new MechanicalService();
        ms.setName("Troca de óleo");
        ms.setBasePrice(new BigDecimal("150.00"));
        ms.setEstimatedDurationMinutes(30);

        Quote quote = new Quote();

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(mechanicalServiceRepository.findByUuid(msUuid)).thenReturn(Optional.of(ms));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.addServiceToDiagnosis(osUuid, msUuid);

        assertThat(quote.getServiceLines()).hasSize(1);
        assertThat(quote.getServiceLines().get(0).getNameSnapshot()).isEqualTo("Troca de óleo");
        verify(quoteRepository).save(quote);
    }

    @Test
    void addServiceToDiagnosis_throwsWhenServiceNotFound() {
        UUID osUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(mechanicalServiceRepository.findByUuid(msUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.addServiceToDiagnosis(osUuid, msUuid))
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.exception.MechanicalServiceNotFoundException.class);
    }

    // -----------------------------------------------------------------------
    // addProductToDiagnosis
    // -----------------------------------------------------------------------
    @Test
    void addProductToDiagnosis_addsProductLineToQuote() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        Product product = new Product();
        product.setName("Óleo 5W30");
        product.setUnitPrice(new BigDecimal("45.00"));

        Quote quote = new Quote();

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.addProductToDiagnosis(osUuid, productUuid, new BigDecimal("2"));

        assertThat(quote.getProductLines()).hasSize(1);
        assertThat(quote.getProductLines().get(0).getNameSnapshot()).isEqualTo("Óleo 5W30");
        verify(quoteRepository).save(quote);
    }

    @Test
    void addProductToDiagnosis_throwsWhenProductNotFound() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.addProductToDiagnosis(osUuid, productUuid, BigDecimal.ONE))
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException.class);
    }

    @Test
    void addProductToDiagnosis_throwsAndNotifiesAttendantWhenInsufficientStock() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        Product product = new Product();
        product.setName("Óleo 5W30");

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        doThrow(br.com.fiap.pos.tech_challenge.core.exception.InsufficientStockException.class)
                .when(stockService).checkAvailability(productUuid, new BigDecimal("10"));

        assertThatThrownBy(() -> sut.addProductToDiagnosis(osUuid, productUuid, new BigDecimal("10")))
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.exception.InsufficientStockException.class);

        verify(notificationService).publishInsufficientStockNotification(any(), eq(so));
        verify(quoteRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // removeProductFromDiagnosis
    // -----------------------------------------------------------------------
    @Test
    void removeProductFromDiagnosis_removesLineAndRecalculates() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);
        product.setUnitPrice(new BigDecimal("10.00"));

        QuoteProductLine line = new QuoteProductLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPriceSnapshot(new BigDecimal("10.00"));
        line.setUnbudgeted(false);

        Quote quote = new Quote();
        quote.getProductLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.removeProductFromDiagnosis(osUuid, productUuid);

        assertThat(quote.getProductLines()).isEmpty();
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(stockService, never()).debit(any(), any(), any(), any());
    }

    @Test
    void removeProductFromDiagnosis_throwsWhenProductNotInQuote() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Quote quote = new Quote();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> sut.removeProductFromDiagnosis(osUuid, productUuid))
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException.class);
    }

    // -----------------------------------------------------------------------
    // acceptDelivery
    // -----------------------------------------------------------------------
    @Test
    void acceptDelivery_byAttendant_transitionsToDelivered() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        sut.acceptDelivery(uuid, null, null, true);

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
        verify(otpService, never()).validate(any(), any(), any());

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
    }

    @Test
    void acceptDelivery_byOTP_validatesTokenBeforeTransition() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        sut.acceptDelivery(uuid, "52998224725", "token", false);

        verify(otpService).validate(uuid, "52998224725", "token");
        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
    }

    // -----------------------------------------------------------------------
    // rejectDelivery
    // -----------------------------------------------------------------------
    @Test
    void rejectDelivery_resumesInProgressAndCreatesReworkCycle() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);

        QuoteServiceLine serviceLine = new QuoteServiceLine();
        serviceLine.setEstimatedDurationMinutes(60);
        serviceLine.setPriceSnapshot(new BigDecimal("100.00"));
        Quote quote = new Quote();
        quote.getServiceLines().add(serviceLine);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.rejectDelivery(uuid, "52998224725", "token", "peças com defeito");

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        verify(reworkCycleRepository).save(argThat(r -> r.getRejectionReason().equals("peças com defeito")));
        verify(notificationService).publishToRole(any(), any(), any(), eq(so));

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    // -----------------------------------------------------------------------
    // getServiceOrderStatus
    // -----------------------------------------------------------------------
    @Test
    void getServiceOrderStatus_returnsStatusResponse() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        so.setUuid(uuid);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        var result = sut.getServiceOrderStatus(uuid);

        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    // -----------------------------------------------------------------------
    // getServiceOrder
    // -----------------------------------------------------------------------
    @Test
    void getServiceOrder_returnsResponse() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(mapper.toResponse(so)).thenReturn(expected);

        assertThat(sut.getServiceOrder(uuid)).isEqualTo(expected);
    }

    // -----------------------------------------------------------------------
    // removeServiceFromDiagnosis
    // -----------------------------------------------------------------------
    @Test
    void removeServiceFromDiagnosis_removesLineAndRecalculates() {
        UUID osUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();

        MechanicalService ms = new MechanicalService();
        ms.setUuid(msUuid);
        ms.setBasePrice(new BigDecimal("100.00"));
        ms.setEstimatedDurationMinutes(30);

        QuoteServiceLine line = new QuoteServiceLine();
        line.setMechanicalService(ms);
        line.setPriceSnapshot(new BigDecimal("100.00"));

        Quote quote = new Quote();
        quote.getServiceLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.removeServiceFromDiagnosis(osUuid, msUuid);

        assertThat(quote.getServiceLines()).isEmpty();
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(quoteRepository).save(quote);
    }

    @Test
    void removeServiceFromDiagnosis_throwsWhenServiceNotInQuote() {
        UUID osUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();

        Quote quote = new Quote();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> sut.removeServiceFromDiagnosis(osUuid, msUuid))
                .isInstanceOf(MechanicalServiceNotFoundException.class);
    }

    // -----------------------------------------------------------------------
    // completeDiagnosis
    // -----------------------------------------------------------------------
    @Test
    void completeDiagnosis_transitionsToAwaitingApprovalAndSendsOTP() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        Quote quote = new Quote();
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(quote)).thenReturn(quote);
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(expected);

        ServiceOrderResponse result = sut.completeDiagnosis(uuid);

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(so.getApprovalExpiresAt()).isNotNull();
        verify(otpService).generateAndSend(so);
        assertThat(result).isNotNull();

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void completeDiagnosis_throwsWhenNotInDiagnosis() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.completeDiagnosis(uuid))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // -----------------------------------------------------------------------
    // requestProduct
    // -----------------------------------------------------------------------
    @Test
    void requestProduct_debitsAndTriggersApprovalWhenInProgress() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);
        product.setName("Filtro de ar");
        product.setUnitPrice(new BigDecimal("30.00"));

        Quote quote = new Quote();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getId()).thenReturn(1L);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.requestProduct(osUuid, productUuid, BigDecimal.ONE, principal);

        verify(stockService).debit(eq(productUuid), eq(BigDecimal.ONE), eq(so), isNull());
        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(so.getApprovalExpiresAt()).isNotNull();
        verify(otpService).generateAndSend(so);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void requestProduct_addendumPhase_debitsAndDoesNotChangeStatus() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);
        product.setName("Pastilha de freio");
        product.setUnitPrice(new BigDecimal("80.00"));

        Quote quote = new Quote();
        quote.setApprovedAt(Instant.now().minusSeconds(3600));

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getId()).thenReturn(2L);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.requestProduct(osUuid, productUuid, BigDecimal.ONE, principal);

        verify(stockService).debit(eq(productUuid), eq(BigDecimal.ONE), eq(so), isNull());
        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        verify(otpService, never()).generateAndSend(any());
        verify(serviceOrderRepository, never()).save(any());
    }

    @Test
    void requestProduct_throwsWhenProductNotFound() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.requestProduct(osUuid, productUuid, BigDecimal.ONE, null))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void requestProduct_notifiesAndThrowsWhenInsufficientStock() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        doThrow(new InsufficientStockException()).when(stockService).debit(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.requestProduct(osUuid, productUuid, BigDecimal.ONE, principal))
                .isInstanceOf(InsufficientStockException.class);

        verify(notificationService).publishInsufficientStockNotification(any(), eq(so));
    }

    // -----------------------------------------------------------------------
    // closeDispute — from IN_PROGRESS
    // -----------------------------------------------------------------------
    @Test
    void closeDispute_succeedsFromInProgressStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.closeDispute(uuid, "motivo", null);

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DISPUTED);
        assertThat(result).isNotNull();
    }

    // -----------------------------------------------------------------------
    // approveQuote — debit throws InsufficientStockException (fail-open)
    // -----------------------------------------------------------------------
    @Test
    void approveQuote_notifiesAndContinuesWhenDebitThrowsInsufficientStock() {
        UUID uuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine line = new QuoteProductLine();
        line.setProduct(product);
        line.setQuantity(BigDecimal.ONE);
        line.setUnbudgeted(false);

        Quote quote = new Quote();
        quote.getProductLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(responseFor(so));
        doThrow(new InsufficientStockException()).when(stockService).debit(any(), any(), any(), isNull());

        ServiceOrderResponse result = sut.approveQuote(uuid, "52998224725", "token");

        verify(notificationService).publishInsufficientStockNotification(any(), eq(so));
        assertThat(result).isNotNull();
    }

    // -----------------------------------------------------------------------
    // returnProduct — product not in quote
    // -----------------------------------------------------------------------
    @Test
    void returnProduct_throwsWhenProductNotInQuote() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Quote quote = new Quote(); // empty product lines

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        UserDetailsImpl principal = mock(UserDetailsImpl.class);
        when(principal.getLogin()).thenReturn("mecanico");

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> sut.returnProduct(osUuid, productUuid, "pass", principal))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private ServiceOrder serviceOrderWithStatus(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setStatus(status);
        return so;
    }

    private ServiceOrderStatusChangedEvent publishedStatusEvent() {
        ArgumentCaptor<ServiceOrderStatusChangedEvent> captor =
                ArgumentCaptor.forClass(ServiceOrderStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private ServiceOrderResponse responseFor(ServiceOrder so) {
        return new ServiceOrderResponse(UUID.randomUUID(), so.getStatus(),
                "queixa", null, LocalDateTime.now());
    }
}
