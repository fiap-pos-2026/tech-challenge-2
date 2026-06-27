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

    @Query("""
            SELECT so FROM ServiceOrder so
            WHERE (:status IS NULL OR so.status = :status)
              AND (:customerUuid IS NULL OR so.customer.uuid = :customerUuid)
              AND (:from IS NULL OR so.createdAt >= :from)
              AND (:to IS NULL OR so.createdAt <= :to)
            """)
    Page<ServiceOrder> findWithFilters(
            @Param("status") ServiceOrderStatus status,
            @Param("customerUuid") UUID customerUuid,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
