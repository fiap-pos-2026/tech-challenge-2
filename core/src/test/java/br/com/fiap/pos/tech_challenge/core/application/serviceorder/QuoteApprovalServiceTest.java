package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.StockService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
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
class QuoteApprovalServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;
    @Mock StockService stockService;
    @Mock NotificationService notificationService;
    @Mock OTPService otpService;
    @Mock CurrentActorPort currentActorPort;

    QuoteApprovalService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new QuoteApprovalService(otpService, stockService, notificationService,
                currentActorPort, quotes, store, responseFactory);
    }

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
        quote.setApprovedAt(Instant.now().minusSeconds(3600));
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

    @Test
    void rejectQuote_cancelledWhenNoUnbudgetedLines() {
        UUID uuid = UUID.randomUUID();

        Quote quote = new Quote();

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(responseFor(so));

        sut.rejectQuote(uuid, "52998224725", "token");

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
        verify(serviceOrderRepository).save(so);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
    }

    @Test
    void rejectQuote_compensatesStockAndResumesWhenAddendum() {
        UUID uuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Product product = new Product();
        product.setUuid(productUuid);

        QuoteProductLine line = new QuoteProductLine();
        line.setId(1L);
        line.setProduct(product);
        line.setQuantity(BigDecimal.TWO);
        line.setUnitPriceSnapshot(BigDecimal.TEN);
        line.setUnbudgeted(true);

        Quote quote = new Quote();
        quote.getProductLines().add(line);

        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(serviceOrderRepository.save(any())).thenReturn(so);
        when(mapper.toResponse(any())).thenReturn(responseFor(so));

        sut.rejectQuote(uuid, "52998224725", "token");

        verify(stockService).compensate(eq(productUuid), eq(BigDecimal.TWO), eq(so), any());
        verify(quoteRepository).save(quote);
        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
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
