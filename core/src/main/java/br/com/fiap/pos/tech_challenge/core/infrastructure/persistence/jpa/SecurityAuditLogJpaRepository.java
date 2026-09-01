package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.SecurityAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface SecurityAuditLogJpaRepository extends JpaRepository<SecurityAuditLogEntity, Long> {
}
