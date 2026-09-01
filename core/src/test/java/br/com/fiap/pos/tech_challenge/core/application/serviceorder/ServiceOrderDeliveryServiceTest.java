package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ReworkCycleRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteServiceLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderDeliveryServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;
    @Mock ReworkCycleRepository reworkCycleRepository;
    @Mock OTPService otpService;
    @Mock NotificationService notificationService;
    @Mock CurrentActorPort currentActorPort;

    ServiceOrderDeliveryService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new ServiceOrderDeliveryService(reworkCycleRepository, otpService, notificationService,
                currentActorPort, quotes, store, responseFactory);
        ReflectionTestUtils.setField(sut, "reworkHourlyRate", new BigDecimal("150.00"));
    }

    @Test
    void acceptDelivery_byAttendant_transitionsToDelivered() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        ServiceOrderResponse expected = responseFor(so);

        User actor = new User();
        actor.setLogin("atendente");
        when(currentActorPort.currentUser()).thenReturn(Optional.of(actor));
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        sut.acceptDelivery(uuid, null, null);

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

        sut.acceptDelivery(uuid, "52998224725", "token");

        verify(otpService).validate(uuid, "52998224725", "token");
        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
    }

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
