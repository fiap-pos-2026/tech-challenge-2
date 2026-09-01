package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StockMovementRepository {
    Page<StockMovement> findAll(Pageable pageable);
    Page<StockMovement> findAllByProductId(Long productId, Pageable pageable);
    Page<StockMovement> findAllByServiceOrderId(Long serviceOrderId, Pageable pageable);
    Optional<StockMovement> findById(Long id);
    StockMovement save(StockMovement movement);
    void deleteAll();
}
