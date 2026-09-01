package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByLogin(String login);
    Optional<User> findByUuid(UUID uuid);
    Optional<User> findById(Long id);
    List<User> findAll();
    List<User> findByRole(UserRole role);
    boolean existsByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(UserRole role);
    void updateLastLogin(Long id);
    User save(User user);
    void deleteById(Long id);
    void deleteAll();
}
