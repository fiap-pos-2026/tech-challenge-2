package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.AuditLogService;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderDisputeServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;
    @Mock NotificationService notificationService;
    @Mock AuditLogService auditLogService;
    @Mock CurrentActorPort currentActorPort;

    ServiceOrderDisputeService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new ServiceOrderDisputeService(notificationService, auditLogService,
                currentActorPort, store, responseFactory);
    }

    @Test
    void closeDispute_throwsFromTerminalStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.DELIVERED);
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> sut.closeDispute(uuid, "resolução"))
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

        ServiceOrderResponse result = sut.closeDispute(uuid, "resolução");

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DISPUTED);
        assertThat(result).isNotNull();

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.DISPUTED);
    }

    @Test
    void closeDispute_succeedsFromInProgressStatus() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(serviceOrderRepository.save(so)).thenReturn(so);
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.closeDispute(uuid, "motivo");

        assertThat(so.getStatus()).isEqualTo(ServiceOrderStatus.DISPUTED);
        assertThat(result).isNotNull();
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
