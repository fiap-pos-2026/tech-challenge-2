package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.MechanicalServiceJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.web.dto.ServiceAvgDurationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class MechanicalServiceRepositoryAdapter implements MechanicalServiceRepository {

    private final MechanicalServiceJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<MechanicalService> findByUuid(UUID uuid) { return jpa.findByUuid(uuid).map(mapper::toDomain); }
    public Page<MechanicalService> findAll(Pageable pageable) { return jpa.findAll(pageable).map(mapper::toDomain); }
    public boolean existsByIdAndServiceOrders_StatusIn(Long id, Collection<ServiceOrderStatus> statuses) {
        return jpa.existsByIdAndServiceOrders_StatusIn(id, statuses);
    }
    public List<ServiceAvgDurationResponse> findAvgDurationByService(Collection<ServiceOrderStatus> completedStatuses) {
        return jpa.findAvgDurationByService(completedStatuses);
    }
    public MechanicalService save(MechanicalService service) { return mapper.toDomain(jpa.save(mapper.toEntity(service))); }
    public void deleteById(Long id) { jpa.deleteById(id); }
}
