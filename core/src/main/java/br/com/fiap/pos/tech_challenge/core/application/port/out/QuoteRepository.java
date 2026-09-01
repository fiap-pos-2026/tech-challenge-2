package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;

import java.util.Optional;

public interface QuoteRepository {
    Optional<Quote> findByServiceOrderId(Long serviceOrderId);
    Optional<Quote> findFirstByServiceOrderIdOrderByCreatedAtDesc(Long serviceOrderId);
    Quote save(Quote quote);
}
