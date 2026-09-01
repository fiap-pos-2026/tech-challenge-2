package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.ProductJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<Product> findByUuid(UUID uuid) { return jpa.findByUuid(uuid).map(mapper::toDomain); }
    public Optional<Product> findByUuidForUpdate(UUID uuid) { return jpa.findByUuidForUpdate(uuid).map(mapper::toDomain); }
    public Page<Product> findAll(Pageable pageable) { return jpa.findAll(pageable).map(mapper::toDomain); }
    public boolean existsByIdAndServiceOrders_StatusIn(Long id, Collection<ServiceOrderStatus> statuses) {
        return jpa.existsByIdAndServiceOrders_StatusIn(id, statuses);
    }
    public Product save(Product product) { return mapper.toDomain(jpa.save(mapper.toEntity(product))); }
    public void deleteById(Long id) { jpa.deleteById(id); }
    public void deleteAll() { jpa.deleteAll(); }
}
