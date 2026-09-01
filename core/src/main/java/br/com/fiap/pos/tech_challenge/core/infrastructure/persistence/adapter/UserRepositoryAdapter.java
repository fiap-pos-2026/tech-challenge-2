package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.UserJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<User> findByLogin(String login) { return jpa.findByLogin(login).map(mapper::toDomain); }
    public Optional<User> findByUuid(UUID uuid) { return jpa.findByUuid(uuid).map(mapper::toDomain); }
    public Optional<User> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    public List<User> findAll() { return jpa.findAll().stream().map(mapper::toDomain).toList(); }
    public List<User> findByRole(UserRole role) { return jpa.findByRole(role).stream().map(mapper::toDomain).toList(); }
    public boolean existsByLoginIgnoreCase(String login) { return jpa.existsByLoginIgnoreCase(login); }
    public boolean existsByEmailIgnoreCase(String email) { return jpa.existsByEmailIgnoreCase(email); }
    public long countByRole(UserRole role) { return jpa.countByRole(role); }
    public void updateLastLogin(Long id) { jpa.updateLastLogin(id); }
    public User save(User user) { return mapper.toDomain(jpa.save(mapper.toEntity(user))); }
    public void deleteById(Long id) { jpa.deleteById(id); }
    public void deleteAll() { jpa.deleteAll(); }
}
