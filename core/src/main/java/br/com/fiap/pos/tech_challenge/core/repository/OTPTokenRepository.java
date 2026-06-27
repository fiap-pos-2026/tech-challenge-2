package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.OTPToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface OTPTokenRepository extends JpaRepository<OTPToken, Long> {

    Optional<OTPToken> findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
            Long serviceOrderId, Instant now);
}
