package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Product> findByUuid(UUID uuid);
    Optional<Product> findByUuidForUpdate(UUID uuid);
    Page<Product> findAll(Pageable pageable);
    boolean existsByIdAndServiceOrders_StatusIn(Long id, Collection<ServiceOrderStatus> statuses);
    Product save(Product product);
    void deleteById(Long id);
    void deleteAll();
}
