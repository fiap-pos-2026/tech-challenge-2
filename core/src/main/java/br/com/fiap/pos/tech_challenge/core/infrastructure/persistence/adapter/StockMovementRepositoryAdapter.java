package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.StockMovementRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.StockMovement;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.StockMovementJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class StockMovementRepositoryAdapter implements StockMovementRepository {

    private final StockMovementJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Page<StockMovement> findAll(Pageable pageable) { return jpa.findAll(pageable).map(mapper::toDomain); }
    public Page<StockMovement> findAllByProductId(Long productId, Pageable pageable) {
        return jpa.findAllByProductId(productId, pageable).map(mapper::toDomain);
    }
    public Page<StockMovement> findAllByServiceOrderId(Long serviceOrderId, Pageable pageable) {
        return jpa.findAllByServiceOrderId(serviceOrderId, pageable).map(mapper::toDomain);
    }
    public Optional<StockMovement> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    public StockMovement save(StockMovement movement) { return mapper.toDomain(jpa.save(mapper.toEntity(movement))); }
    public void deleteAll() { jpa.deleteAll(); }
}
