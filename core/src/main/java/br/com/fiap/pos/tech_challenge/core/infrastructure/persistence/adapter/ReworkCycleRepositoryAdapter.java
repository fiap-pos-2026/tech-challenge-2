package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.ReworkCycleRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.ReworkCycle;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.ReworkCycleJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@RequiredArgsConstructor
@Transactional
public class ReworkCycleRepositoryAdapter implements ReworkCycleRepository {

    private final ReworkCycleJpaRepository jpa;
    private final PersistenceMapper mapper;

    public ReworkCycle save(ReworkCycle reworkCycle) { return mapper.toDomain(jpa.save(mapper.toEntity(reworkCycle))); }
}
