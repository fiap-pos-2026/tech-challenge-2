package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "serviceOrder")
    Page<StockMovementEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "serviceOrder")
    Page<StockMovementEntity> findAllByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = "serviceOrder")
    Page<StockMovementEntity> findAllByServiceOrderId(Long serviceOrderId, Pageable pageable);
}
