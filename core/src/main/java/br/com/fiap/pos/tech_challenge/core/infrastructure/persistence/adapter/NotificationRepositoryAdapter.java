package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.NotificationRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.Notification;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.NotificationJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(mapper::toDomain);
    }
    public Optional<Notification> findByUuidAndUserId(UUID uuid, Long userId) {
        return jpa.findByUuidAndUserId(uuid, userId).map(mapper::toDomain);
    }
    public void deleteByReadTrueAndReadAtBefore(Instant cutoff) { jpa.deleteByReadTrueAndReadAtBefore(cutoff); }
    public Notification save(Notification n) { return mapper.toDomain(jpa.save(mapper.toEntity(n))); }
}
