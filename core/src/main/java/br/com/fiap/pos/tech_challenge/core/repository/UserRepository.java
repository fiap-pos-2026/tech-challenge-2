package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    @Query("UPDATE User u SET u.lastLogin = current_timestamp WHERE u.id = ?1")
    @Modifying
    void updateLastLogin(Long id);

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(UserRole role);
}
