package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.StockService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteServiceLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderDiagnosisServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;
    @Mock MechanicalServiceRepository mechanicalServiceRepository;
    @Mock ProductRepository productRepository;
    @Mock StockService stockService;
    @Mock NotificationService notificationService;
    @Mock OTPService otpService;

    ServiceOrderDiagnosisService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new ServiceOrderDiagnosisService(mechanicalServiceRepository, productRepository, stockService,
                notificationService, otpService, quotes, store, responseFactory);
    }

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

    @Test
    void startDiagnosis_throwsFromCancelledStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.CANCELLED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.startDiagnosis(uuid))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

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
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException.class);
    }

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
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException.class);
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
        doThrow(br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException.class)
                .when(stockService).checkAvailability(productUuid, new BigDecimal("10"));

        assertThatThrownBy(() -> sut.addProductToDiagnosis(osUuid, productUuid, new BigDecimal("10")))
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException.class);

        verify(notificationService).publishInsufficientStockNotification(any(), eq(so));
        verify(quoteRepository, never()).save(any());
    }

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
                .isInstanceOf(br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException.class);
    }

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
