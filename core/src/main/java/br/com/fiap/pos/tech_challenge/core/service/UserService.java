package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.ChangePasswordDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.mapper.UserMapper;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.util.AuthUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final int MAX_PASSWORD_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository repository;

    private final UserMapper mapper;

    private final AuditLogService auditLogService;

    private final PasswordEncoder passwordEncoder;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByLogin(username)
                .map(UserDetailsImpl::new)
                .orElseThrow(() -> new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return mapper.toDTOs(repository.findAll());
    }

    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLoginState(User user) {
        repository.save(user);
    }

    @Transactional
    public void updateLastLogin(final Long id) {
        repository.updateLastLogin(id);
    }

    @Transactional
    public UserDTO create(final CreateUserDTO dto) {
        this.validate(dto);
        final User user = mapper.toEntity(dto);
        return mapper.toDTO(repository.save(user));
    }

    public void validate(final CreateUserDTO dto) {
        if (repository.existsByLoginIgnoreCase(dto.login())) {
            throw new CoreException(EApplicationError.LOGIN_ALREADY_EXISTS);
        }

        if (repository.existsByEmailIgnoreCase(dto.email())) {
            throw new CoreException(EApplicationError.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void deleteById(final Long id) {
        User target = repository.findById(id)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));

        if (target.getRole() == UserRole.ADMIN && repository.countByRole(UserRole.ADMIN) == 1) {
            throw new CoreException(EApplicationError.LAST_ADMIN_DELETION_NOT_ALLOWED);
        }

        repository.deleteById(id);

        UserDetailsImpl loggedUser = AuthUtility.getLoggedUser();
        auditLogService.register(AuditEventType.USER_DELETED, loggedUser, target.getLogin(), "SUCCESS",
                "User deleted by admin: " + loggedUser.getUsername());
    }

    @Transactional
    public UserDTO update(final Long id, final UpdateUserDTO dto) {
        User current = repository.findById(id)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));

        UserDetailsImpl loggedUser = AuthUtility.getLoggedUser();
        if (loggedUser.getId().equals(id)
                && loggedUser.getRole() == UserRole.ADMIN
                && dto.role() != null
                && dto.role() != UserRole.ADMIN) {
            throw new CoreException(EApplicationError.ADMIN_SELF_ROLE_CHANGE_NOT_ALLOWED);
        }

        if (!Strings.CI.equals(current.getLogin(), dto.login()) &&
            repository.existsByLoginIgnoreCase(dto.login())) {
            throw new CoreException(EApplicationError.LOGIN_ALREADY_EXISTS);
        }

        if (!Strings.CI.equals(current.getEmail(), dto.email()) &&
            repository.existsByEmailIgnoreCase(dto.email())) {
            throw new CoreException(EApplicationError.EMAIL_ALREADY_EXISTS);
        }

        boolean roleChanged = dto.role() != null && dto.role() != current.getRole();

        User toUpdate = mapper.fullUpdate(dto, current);
        UserDTO result = mapper.toDTO(repository.save(toUpdate));

        if (roleChanged) {
            auditLogService.register(AuditEventType.USER_ROLE_CHANGED, loggedUser, current.getLogin(), "SUCCESS",
                    "Role changed from " + current.getRole() + " to " + dto.role() + " by admin: " + loggedUser.getUsername());
        }

        return result;
    }

    @Transactional
    public void changePassword(final Long userId, final ChangePasswordDTO dto) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));

        // OWASP A07 — bloqueia após tentativas repetidas de senha atual errada
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new CoreException(EApplicationError.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(dto.senhaAtual(), user.getPassword())) {
            int attempts = user.getLoginFailedAttempts() + 1;
            user.setLoginFailedAttempts(attempts);
            if (attempts >= MAX_PASSWORD_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            }
            repository.save(user);
            auditLogService.register(AuditEventType.PASSWORD_CHANGE_FAILED, user,
                    user.getLogin(), "401", "Tentativa " + attempts + " com senha atual incorreta");
            throw new CoreException(EApplicationError.WRONG_CURRENT_PASSWORD);
        }

        // OWASP A07 — impede reutilização imediata da senha atual
        if (passwordEncoder.matches(dto.novaSenha(), user.getPassword())) {
            throw new CoreException(EApplicationError.SAME_PASSWORD);
        }

        user.setLoginFailedAttempts(0);
        user.setLockedUntil(null);
        user.setPassword(passwordEncoder.encode(dto.novaSenha()));
        user.setForceChangePassword(false);
        repository.save(user);

        UserDetailsImpl loggedUser = AuthUtility.getLoggedUser();
        auditLogService.register(AuditEventType.PASSWORD_CHANGED, loggedUser,
                user.getLogin(), "SUCCESS", "Password changed by user: " + loggedUser.getUsername());
    }

    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        return repository.findByLogin(login)
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserDTO getLoggedUser() {
        UserDetailsImpl loggedUser = AuthUtility.getLoggedUser();

        User entity = repository.findById(loggedUser.getId())
                .orElseThrow(() -> new CoreException(EApplicationError.USER_NOT_FOUND));

        return mapper.toDTO(entity);
    }
}
