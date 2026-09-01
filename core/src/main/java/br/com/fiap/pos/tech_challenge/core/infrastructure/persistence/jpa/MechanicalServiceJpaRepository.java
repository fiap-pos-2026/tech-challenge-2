package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.MechanicalServiceEntity;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface MechanicalServiceJpaRepository extends JpaRepository<MechanicalServiceEntity, Long> {

    Optional<MechanicalServiceEntity> findByUuid(UUID uuid);

    @Query("""
            SELECT CASE WHEN COUNT(qsl) > 0 THEN true ELSE false END
            FROM QuoteServiceLineEntity qsl
            WHERE qsl.mechanicalService.id = :id
              AND qsl.quote.serviceOrder.status IN :statuses
            """)
    boolean existsByIdAndServiceOrders_StatusIn(@Param("id") Long id,
                                                @Param("statuses") Collection<ServiceOrderStatus> statuses);

    @Query("""
            SELECT new br.com.fiap.pos.tech_challenge.core.application.dto.ServiceAvgDurationResponse(
                qsl.mechanicalService.uuid,
                qsl.nameSnapshot,
                AVG(qsl.estimatedDurationMinutes),
                COUNT(qsl))
            FROM QuoteServiceLineEntity qsl
            WHERE qsl.quote.serviceOrder.status IN :completedStatuses
            GROUP BY qsl.mechanicalService.uuid, qsl.nameSnapshot
            ORDER BY qsl.nameSnapshot
            """)
    List<br.com.fiap.pos.tech_challenge.core.application.dto.ServiceAvgDurationResponse> findAvgDurationByService(
            @Param("completedStatuses") Collection<ServiceOrderStatus> completedStatuses);
}
