package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.AuditLogService;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderDisputeService {

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CurrentActorPort currentActorPort;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional
    public ServiceOrderResponse closeDispute(UUID osUuid, String resolution) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        if (so.getStatus() != ServiceOrderStatus.IN_PROGRESS
                && so.getStatus() != ServiceOrderStatus.COMPLETED) {
            throw new InvalidStatusTransitionException();
        }

        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.DISPUTED);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ORDER_DISPUTED,
                "OS " + osUuid + " encerrada como DISPUTED. Resolução: " + resolution, saved);

        User actor = currentActorPort.currentUser().orElse(null);
        auditLogService.register(AuditEventType.DISPUTED_CLOSURE, actor,
                actor != null ? actor.getLogin() : null, "200",
                "OS " + osUuid + " encerrada como DISPUTED");

        return responseFactory.toResponse(saved);
    }
}
