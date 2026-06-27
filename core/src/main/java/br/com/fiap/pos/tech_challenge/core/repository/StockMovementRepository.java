package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findAllByProductId(Long productId, Pageable pageable);

    Page<StockMovement> findAllByServiceOrderId(Long serviceOrderId, Pageable pageable);
}
