package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.UserEntity;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLogin(String login);

    Optional<UserEntity> findByUuid(UUID uuid);

    @Query("UPDATE UserEntity u SET u.lastLogin = current_timestamp WHERE u.id = ?1")
    @Modifying
    void updateLastLogin(Long id);

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findByRole(UserRole role);

    long countByRole(UserRole role);
}
