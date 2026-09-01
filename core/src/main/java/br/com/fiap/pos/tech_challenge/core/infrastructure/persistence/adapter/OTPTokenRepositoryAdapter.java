package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.OTPTokenRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.OTPToken;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.OTPTokenJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class OTPTokenRepositoryAdapter implements OTPTokenRepository {

    private final OTPTokenJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<OTPToken> findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
            Long serviceOrderId, Instant now) {
        return jpa.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(serviceOrderId, now)
                .map(mapper::toDomain);
    }
    public OTPToken save(OTPToken token) { return mapper.toDomain(jpa.save(mapper.toEntity(token))); }
}
