package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.QuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface QuoteJpaRepository extends JpaRepository<QuoteEntity, Long> {

    Optional<QuoteEntity> findByServiceOrderId(Long serviceOrderId);

    Optional<QuoteEntity> findFirstByServiceOrderIdOrderByCreatedAtDesc(Long serviceOrderId);
}
