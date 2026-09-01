package br.com.fiap.pos.tech_challenge.core.application.scheduler;

import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class QuoteExpirationScheduler {

    private final ServiceOrderRepository serviceOrderRepository;
    private final OTPService otpService;
    private final NotificationService notificationService;
    private final DomainEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireOverdueQuotes() {
        List<ServiceOrder> expired = serviceOrderRepository
                .findByStatusAndApprovalExpiresAtBefore(ServiceOrderStatus.AWAITING_APPROVAL, Instant.now());

        for (ServiceOrder so : expired) {
            ServiceOrderStatus previousStatus = so.getStatus();
            so.setStatus(ServiceOrderStatus.CANCELLED);
            otpService.invalidateByServiceOrder(so);
            notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.QUOTE_EXPIRED,
                    "OS " + so.getUuid() + " cancelada por expiração do orçamento.", so);
            publishStatusChanged(so, previousStatus);
            log.info("OS {} cancelada por expiração do orçamento.", so.getUuid());
        }

        serviceOrderRepository.saveAll(expired);
    }

    private void publishStatusChanged(ServiceOrder so, ServiceOrderStatus previousStatus) {
        Customer customer = so.getCustomer();
        eventPublisher.publish(new ServiceOrderStatusChangedEvent(
                so.getUuid(), previousStatus, so.getStatus(),
                customer != null ? customer.getEmail() : null,
                customer != null ? customer.getName() : null));
    }
}
