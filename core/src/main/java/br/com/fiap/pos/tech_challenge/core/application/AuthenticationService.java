package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.web.dto.LoginDTO;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.exception.AccountInactiveException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.AccountLockedException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.TokenBlacklistPort;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.TokenUtility;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import br.com.fiap.pos.tech_challenge.core.application.port.out.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class AuthenticationService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final TokenUtility tokenUtility;
    private final TokenBlacklistPort tokenBlacklistService;
    private final AuthenticationConfiguration configuration;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordHasher passwordHasher;

    @Transactional
    public TokenDTO authenticate(final LoginDTO dto) {
        User user = userRepository.findByLogin(dto.getLogin()).orElse(null);

        if (user != null) {
            if (!user.isActive()) {
                auditLogService.register(AuditEventType.LOGIN_FAILED, user, dto.getLogin(), "401", "Conta inativa");
                throw new AccountInactiveException();
            }

            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                auditLogService.register(AuditEventType.LOGIN_FAILED, user, dto.getLogin(), "423", "Conta bloqueada");
                throw new AccountLockedException();
            }

            if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
                user.setLockedUntil(null);
                user.setLoginFailedAttempts(0);
            }
        }

        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    dto.getLogin(), dto.getPassword(), Collections.emptyList());

            AuthenticationManager manager = configuration.getAuthenticationManager();
            Authentication authentication = manager.authenticate(authToken);

            UserDetailsImpl impl = (UserDetailsImpl) authentication.getPrincipal();

            if (impl == null) {
                throw new CoreException(EApplicationError.INVALID_PRINCIPAL);
            }

            User authenticatedUser = userRepository.findByLogin(impl.getLogin()).orElseThrow();
            authenticatedUser.setLoginFailedAttempts(0);
            authenticatedUser.setLockedUntil(null);
            userRepository.save(authenticatedUser);

            userService.updateLastLogin(impl.getId());

            TokenDTO token = tokenUtility.generate(impl);

            auditLogService.register(AuditEventType.LOGIN_SUCCESS, authenticatedUser, dto.getLogin(), "200", null);

            return token;

        } catch (CoreException e) {
            throw e;
        } catch (BadCredentialsException _) {
            handleFailedAttempt(dto.getLogin(), user);
            throw new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD);
        }  catch (Exception e) {
            log.error(e.getMessage());
            handleFailedAttempt(dto.getLogin(), user);
            throw new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD);
        }
    }

    public void signout(final String token, final UserDetailsImpl user) {
        String jti = tokenUtility.getJti(token);
        long ttlSeconds = tokenUtility.getRemainingTtlSeconds(token);

        try {
            tokenBlacklistService.invalidate(jti, ttlSeconds);
        } catch (Exception e) {
            log.error("Falha ao invalidar token na blacklist (fail-open): {}", e.getMessage());
        }

        auditLogService.register(AuditEventType.LOGOUT_SUCCESS, user.user(), jti, "204", null);
    }

    @Transactional
    public void validatePassword(String login, String rawPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD));

        if (!passwordHasher.matches(rawPassword, user.getPassword())) {
            auditLogService.register(AuditEventType.REAUTHENTICATION_FAILED, user, login, "401", null);
            throw new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD);
        }

        auditLogService.register(AuditEventType.REAUTHENTICATION_SUCCESS, user, login, "200", null);
    }

    private void handleFailedAttempt(String login, User user) {
        if (user == null) {
            auditLogService.register(AuditEventType.LOGIN_FAILED, null, login, "401", "Usuário não encontrado");
            return;
        }

        int attempts = user.getLoginFailedAttempts() + 1;
        user.setLoginFailedAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            auditLogService.register(AuditEventType.OTP_INVALID_LIMIT, user, login, "423", "Conta bloqueada por " + LOCK_DURATION_MINUTES + " minutos");
        } else {
            auditLogService.register(AuditEventType.LOGIN_FAILED, user, login, "401", "Tentativa " + attempts);
        }

        userService.saveLoginState(user);
    }
}
