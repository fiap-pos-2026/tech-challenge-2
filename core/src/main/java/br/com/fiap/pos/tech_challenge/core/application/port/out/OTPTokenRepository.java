package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.OTPToken;

import java.time.Instant;
import java.util.Optional;

public interface OTPTokenRepository {
    Optional<OTPToken> findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
            Long serviceOrderId, Instant now);
    OTPToken save(OTPToken token);
}
