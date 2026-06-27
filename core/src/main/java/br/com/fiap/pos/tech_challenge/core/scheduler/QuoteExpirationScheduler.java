package br.com.fiap.pos.tech_challenge.core.scheduler;

import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.repository.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.service.NotificationService;
import br.com.fiap.pos.tech_challenge.core.service.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireOverdueQuotes() {
        List<ServiceOrder> expired = serviceOrderRepository
                .findByStatusAndApprovalExpiresAtBefore(ServiceOrderStatus.AWAITING_APPROVAL, Instant.now());

        for (ServiceOrder so : expired) {
            so.setStatus(ServiceOrderStatus.CANCELLED);
            otpService.invalidateByServiceOrder(so);
            notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.QUOTE_EXPIRED,
                    "OS " + so.getUuid() + " cancelada por expiração do orçamento.", so);
            log.info("OS {} cancelada por expiração do orçamento.", so.getUuid());
        }

        serviceOrderRepository.saveAll(expired);
    }
}
