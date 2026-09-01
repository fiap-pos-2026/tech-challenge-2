package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

    @EntityGraph(attributePaths = "serviceOrderRef")
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<NotificationEntity> findByUuidAndUserId(UUID uuid, Long userId);

    void deleteByReadTrueAndReadAtBefore(Instant cutoff);
}
