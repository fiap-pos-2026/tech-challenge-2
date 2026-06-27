package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.Product;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByUuid(UUID uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.uuid = :uuid")
    Optional<Product> findByUuidForUpdate(UUID uuid);

    @Query("""
            SELECT CASE WHEN COUNT(qpl) > 0 THEN true ELSE false END
            FROM QuoteProductLine qpl
            WHERE qpl.product.id = :id
              AND qpl.quote.serviceOrder.status IN :statuses
            """)
    boolean existsByIdAndServiceOrders_StatusIn(@Param("id") Long id,
                                                @Param("statuses") Collection<ServiceOrderStatus> statuses);
}
