package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrderRepository {
    Optional<ServiceOrder> findByUuid(UUID uuid);
    List<ServiceOrder> findByStatusAndApprovalExpiresAtBefore(ServiceOrderStatus status, Instant cutoff);
    Page<ServiceOrder> findWithFilters(ServiceOrderStatus status, UUID customerUuid,
                                       LocalDateTime from, LocalDateTime to, Pageable pageable);
    long count();
    ServiceOrder save(ServiceOrder serviceOrder);
    List<ServiceOrder> saveAll(List<ServiceOrder> serviceOrders);
    void deleteAll();
}
