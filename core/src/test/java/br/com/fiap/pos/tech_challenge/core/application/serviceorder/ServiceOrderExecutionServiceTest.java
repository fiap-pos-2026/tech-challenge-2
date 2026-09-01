package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.AuditLogService;
import br.com.fiap.pos.tech_challenge.core.application.AuthenticationService;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.StockService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ReturnNotAllowedException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderExecutionServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;
    @Mock ProductRepository productRepository;
    @Mock StockService stockService;
    @Mock NotificationService notificationService;
    @Mock OTPService otpService;
    @Mock AuthenticationService authenticationService;
    @Mock AuditLogService auditLogService;
    @Mock CurrentActorPort currentActorPort;

    ServiceOrderExecutionService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new ServiceOrderExecutionService(productRepository, stockService, notificationService, otpService,
                authenticationService, auditLogService, currentActorPort, quotes, store, responseFactory);
    }

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

        User actor = new User();
        actor.setLogin("atendente");
        when(currentActorPort.currentUser()).thenReturn(Optional.of(actor));

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.returnProduct(osUuid, productUuid, password);

        verify(authenticationService).validatePassword("atendente", password);
        verify(stockService).credit(eq(productUuid), eq(BigDecimal.TWO), eq(so), any());
    }

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

        User actor = new User();
        actor.setLogin("mecanico");
        when(currentActorPort.currentUser()).thenReturn(Optional.of(actor));

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        doThrow(new ReturnNotAllowedException()).when(stockService)
                .credit(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.returnProduct(osUuid, productUuid, "pass"))
                .isInstanceOf(ReturnNotAllowedException.class);
    }

    @Test
    void returnProduct_throwsWhenProductNotInQuote() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Quote quote = new Quote();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        User actor = new User();
        actor.setLogin("mecanico");
        when(currentActorPort.currentUser()).thenReturn(Optional.of(actor));

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> sut.returnProduct(osUuid, productUuid, "pass"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void requestProduct_throwsWhenCalledDuringInitialAwaitingApproval() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        Quote quote = new Quote();

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> sut.requestProduct(osUuid, productUuid, BigDecimal.ONE))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(stockService, never()).debit(any(), any(), any(), any());
    }

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

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.requestProduct(osUuid, productUuid, BigDecimal.ONE);

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

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));

        sut.requestProduct(osUuid, productUuid, BigDecimal.ONE);

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

        assertThatThrownBy(() -> sut.requestProduct(osUuid, productUuid, BigDecimal.ONE))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void requestProduct_notifiesAndThrowsWhenInsufficientStock() {
        UUID osUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        doThrow(new InsufficientStockException()).when(stockService).debit(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.requestProduct(osUuid, productUuid, BigDecimal.ONE))
                .isInstanceOf(InsufficientStockException.class);

        verify(notificationService).publishInsufficientStockNotification(any(), eq(so));
    }

    private ServiceOrder serviceOrderWithStatus(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setStatus(status);
        return so;
    }

    private ServiceOrderStatusChangedEvent publishedStatusEvent() {
        ArgumentCaptor<ServiceOrderStatusChangedEvent> captor =
                ArgumentCaptor.forClass(ServiceOrderStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    private ServiceOrderResponse responseFor(ServiceOrder so) {
        return new ServiceOrderResponse(UUID.randomUUID(), so.getStatus(),
                "queixa", null, LocalDateTime.now());
    }
}
