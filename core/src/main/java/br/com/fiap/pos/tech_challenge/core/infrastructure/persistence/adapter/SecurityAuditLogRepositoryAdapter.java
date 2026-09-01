package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.SecurityAuditLogRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.SecurityAuditLog;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.SecurityAuditLogJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@RequiredArgsConstructor
@Transactional
public class SecurityAuditLogRepositoryAdapter implements SecurityAuditLogRepository {

    private final SecurityAuditLogJpaRepository jpa;
    private final PersistenceMapper mapper;

    public SecurityAuditLog save(SecurityAuditLog log) { return mapper.toDomain(jpa.save(mapper.toEntity(log))); }
}
