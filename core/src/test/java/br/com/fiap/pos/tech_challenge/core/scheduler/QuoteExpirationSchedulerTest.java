package br.com.fiap.pos.tech_challenge.core.scheduler;

import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.repository.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.service.NotificationService;
import br.com.fiap.pos.tech_challenge.core.service.OTPService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void expireOverdueQuotes_doesNothingWhenNoneExpired() {
        when(serviceOrderRepository.findByStatusAndApprovalExpiresAtBefore(any(), any()))
                .thenReturn(List.of());

        sut.expireOverdueQuotes();

        verify(otpService, never()).invalidateByServiceOrder(any());
        verify(notificationService, never()).publishToRole(any(), any(), any(), any());
        verify(serviceOrderRepository).saveAll(List.of());
    }

    private ServiceOrder serviceOrderWithStatus(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setUuid(UUID.randomUUID());
        so.setStatus(status);
        return so;
    }
}
