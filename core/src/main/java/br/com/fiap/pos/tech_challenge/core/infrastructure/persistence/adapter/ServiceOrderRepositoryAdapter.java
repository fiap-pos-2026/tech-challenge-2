package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.ServiceOrderJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class ServiceOrderRepositoryAdapter implements ServiceOrderRepository {

    private final ServiceOrderJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<ServiceOrder> findByUuid(UUID uuid) { return jpa.findByUuid(uuid).map(mapper::toDomain); }
    public List<ServiceOrder> findByStatusAndApprovalExpiresAtBefore(ServiceOrderStatus status, Instant cutoff) {
        return mapper.toServiceOrderDomain(jpa.findByStatusAndApprovalExpiresAtBefore(status, cutoff));
    }
    public Page<ServiceOrder> findWithFilters(ServiceOrderStatus status, UUID customerUuid,
                                              LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return jpa.findWithFilters(status, customerUuid, from, to, pageable).map(mapper::toDomain);
    }
    public long count() { return jpa.count(); }
    public ServiceOrder save(ServiceOrder so) { return mapper.toDomain(jpa.save(mapper.toEntity(so))); }
    public List<ServiceOrder> saveAll(List<ServiceOrder> sos) {
        return mapper.toServiceOrderDomain(jpa.saveAll(mapper.toServiceOrderEntity(sos)));
    }
    public void deleteAll() { jpa.deleteAll(); }
}
