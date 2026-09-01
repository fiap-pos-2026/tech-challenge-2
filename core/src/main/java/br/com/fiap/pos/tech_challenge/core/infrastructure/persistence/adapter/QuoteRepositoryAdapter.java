package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.QuoteJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class QuoteRepositoryAdapter implements QuoteRepository {

    private final QuoteJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<Quote> findByServiceOrderId(Long serviceOrderId) {
        return jpa.findByServiceOrderId(serviceOrderId).map(mapper::toDomain);
    }
    public Optional<Quote> findFirstByServiceOrderIdOrderByCreatedAtDesc(Long serviceOrderId) {
        return jpa.findFirstByServiceOrderIdOrderByCreatedAtDesc(serviceOrderId).map(mapper::toDomain);
    }
    public Quote save(Quote quote) { return mapper.toDomain(jpa.save(mapper.toEntity(quote))); }
}
