package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findByServiceOrderId(Long serviceOrderId);

    Optional<Quote> findFirstByServiceOrderIdOrderByCreatedAtDesc(Long serviceOrderId);
}
