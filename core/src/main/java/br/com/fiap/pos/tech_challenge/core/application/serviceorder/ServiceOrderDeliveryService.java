package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ReworkCycleRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteServiceLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ReworkCycle;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderDeliveryService {

    @Value("${app.rework.hourly-rate:150.00}")
    private BigDecimal reworkHourlyRate;

    private final ReworkCycleRepository reworkCycleRepository;
    private final OTPService otpService;
    private final NotificationService notificationService;
    private final CurrentActorPort currentActorPort;
    private final QuoteWorkbench quotes;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional
    public ServiceOrderResponse acceptDelivery(UUID osUuid, String customerDocument, String tokenRaw) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.COMPLETED);

        boolean byAttendantJwt = currentActorPort.currentUser().isPresent();
        if (!byAttendantJwt) {
            otpService.validate(osUuid, customerDocument, tokenRaw);
        }

        return responseFactory.toResponse(store.persistStatusChange(so, ServiceOrderStatus.DELIVERED));
    }

    @Transactional
    public ServiceOrderResponse rejectDelivery(UUID osUuid, String customerDocument, String tokenRaw, String reason) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.COMPLETED);

        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.IN_PROGRESS);

        Quote quote = quotes.latestOrThrow(saved);
        int totalEstimatedMinutes = quote.getServiceLines().stream()
                .mapToInt(QuoteServiceLine::getEstimatedDurationMinutes)
                .sum();
        BigDecimal internalCost = BigDecimal.valueOf(totalEstimatedMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(reworkHourlyRate);

        ReworkCycle rework = new ReworkCycle();
        rework.setServiceOrder(saved);
        rework.setRejectionReason(reason);
        rework.setEstimatedDurationMinutes(totalEstimatedMinutes);
        rework.setEstimatedInternalCost(internalCost);
        reworkCycleRepository.save(rework);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.DELIVERY_REJECTED,
                "Entrega da OS " + osUuid + " rejeitada. Motivo: " + reason, saved);

        return responseFactory.toResponse(saved);
    }
}
