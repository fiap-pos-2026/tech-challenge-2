package br.com.fiap.pos.tech_challenge.core.application.scheduler;

import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class QuoteExpirationSchedulerTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock OTPService otpService;
    @Mock NotificationService notificationService;
    @Mock DomainEventPublisher eventPublisher;

    @InjectMocks QuoteExpirationScheduler sut;

    @Test
    void expireOverdueQuotes_cancelsExpiredOrdersAndNotifies() {
        ServiceOrder so1 = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        ServiceOrder so2 = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        when(serviceOrderRepository.findByStatusAndApprovalExpiresAtBefore(
                eq(ServiceOrderStatus.AWAITING_APPROVAL), any())).thenReturn(List.of(so1, so2));

        sut.expireOverdueQuotes();

        assertThat(so1.getStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
        assertThat(so2.getStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
        verify(otpService, times(2)).invalidateByServiceOrder(any());
        verify(notificationService, times(2)).publishToRole(any(), any(), any(), any());
        verify(serviceOrderRepository).saveAll(any());
    }

    @Test
    void expireOverdueQuotes_publishesStatusEventForEachCancelledOrder() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        Customer customer = new Customer();
        customer.setEmail("cliente@mail.com");
        customer.setName("Cliente Teste");
        so.setCustomer(customer);

        when(serviceOrderRepository.findByStatusAndApprovalExpiresAtBefore(
                eq(ServiceOrderStatus.AWAITING_APPROVAL), any())).thenReturn(List.of(so));

        sut.expireOverdueQuotes();

        ArgumentCaptor<ServiceOrderStatusChangedEvent> captor =
                ArgumentCaptor.forClass(ServiceOrderStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        ServiceOrderStatusChangedEvent event = captor.getValue();
        assertThat(event.serviceOrderUuid()).isEqualTo(so.getUuid());
        assertThat(event.previousStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.CANCELLED);
        assertThat(event.customerEmail()).isEqualTo("cliente@mail.com");
        assertThat(event.customerName()).isEqualTo("Cliente Teste");
    }

    @Test
    void expireOverdueQuotes_doesNothingWhenNoneExpired() {
        when(serviceOrderRepository.findByStatusAndApprovalExpiresAtBefore(any(), any()))
                .thenReturn(List.of());

        sut.expireOverdueQuotes();

        verify(otpService, never()).invalidateByServiceOrder(any());
        verify(notificationService, never()).publishToRole(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any(ServiceOrderStatusChangedEvent.class));
        verify(serviceOrderRepository).saveAll(List.of());
    }

    private ServiceOrder serviceOrderWithStatus(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setUuid(UUID.randomUUID());
        so.setStatus(status);
        return so;
    }
}
