package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
public interface MechanicalServiceRepository extends JpaRepository<MechanicalService, Long> {

    Optional<MechanicalService> findByUuid(UUID uuid);

    @Query("""
            SELECT CASE WHEN COUNT(qsl) > 0 THEN true ELSE false END
            FROM QuoteServiceLine qsl
            WHERE qsl.mechanicalService.id = :id
              AND qsl.quote.serviceOrder.status IN :statuses
            """)
    boolean existsByIdAndServiceOrders_StatusIn(@Param("id") Long id,
                                                @Param("statuses") Collection<ServiceOrderStatus> statuses);
}
