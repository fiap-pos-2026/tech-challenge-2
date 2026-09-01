package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.OTPTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface OTPTokenJpaRepository extends JpaRepository<OTPTokenEntity, Long> {

    Optional<OTPTokenEntity> findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
            Long serviceOrderId, Instant now);
}
