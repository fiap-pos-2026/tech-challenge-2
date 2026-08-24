package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    Optional<ServiceOrder> findByUuid(UUID uuid);

    Page<ServiceOrder> findAllByStatus(ServiceOrderStatus status, Pageable pageable);

    List<ServiceOrder> findByStatusAndApprovalExpiresAtBefore(ServiceOrderStatus status, Instant cutoff);

    @Query(value = """
            SELECT so FROM ServiceOrder so
            WHERE (:status IS NULL OR so.status = :status)
              AND (:status IS NOT NULL OR so.status NOT IN (
                    br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.COMPLETED,
                    br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.DELIVERED))
              AND (:customerUuid IS NULL OR so.customer.uuid = :customerUuid)
              AND (CAST(:from AS TIMESTAMP) IS NULL OR so.createdAt >= :from)
              AND (CAST(:to AS TIMESTAMP) IS NULL OR so.createdAt <= :to)
            ORDER BY CASE
                       WHEN so.status = br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.IN_PROGRESS THEN 1
                       WHEN so.status = br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.AWAITING_APPROVAL THEN 2
                       WHEN so.status = br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.IN_DIAGNOSIS THEN 3
                       WHEN so.status = br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.RECEIVED THEN 4
                       ELSE 5
                     END ASC,
                     so.createdAt ASC
            """,
            countQuery = """
            SELECT COUNT(so) FROM ServiceOrder so
            WHERE (:status IS NULL OR so.status = :status)
              AND (:status IS NOT NULL OR so.status NOT IN (
                    br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.COMPLETED,
                    br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus.DELIVERED))
              AND (:customerUuid IS NULL OR so.customer.uuid = :customerUuid)
              AND (CAST(:from AS TIMESTAMP) IS NULL OR so.createdAt >= :from)
              AND (CAST(:to AS TIMESTAMP) IS NULL OR so.createdAt <= :to)
            """)
    Page<ServiceOrder> findWithFilters(
            @Param("status") ServiceOrderStatus status,
            @Param("customerUuid") UUID customerUuid,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
