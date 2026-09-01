package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ServiceOrderNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ServiceOrderStore {

    private final ServiceOrderRepository serviceOrderRepository;
    private final DomainEventPublisher eventPublisher;

    ServiceOrder findByUuidOrThrow(UUID uuid) {
        return serviceOrderRepository.findByUuid(uuid)
                .orElseThrow(ServiceOrderNotFoundException::new);
    }

    void requireStatus(ServiceOrder so, ServiceOrderStatus required) {
        if (so.getStatus() != required) {
            throw new InvalidStatusTransitionException();
        }
    }

    ServiceOrder persistStatusChange(ServiceOrder so, ServiceOrderStatus newStatus) {
        ServiceOrderStatus previousStatus = so.getStatus();
        so.setStatus(newStatus);
        ServiceOrder saved = serviceOrderRepository.save(so);
        Customer customer = saved.getCustomer();
        eventPublisher.publish(new ServiceOrderStatusChangedEvent(
                saved.getUuid(), previousStatus, saved.getStatus(),
                customer != null ? customer.getEmail() : null,
                customer != null ? customer.getName() : null));
        return saved;
    }
}
