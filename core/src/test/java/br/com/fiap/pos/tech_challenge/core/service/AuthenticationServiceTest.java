package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.LoginDTO;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.exception.AccountInactiveException;
import br.com.fiap.pos.tech_challenge.core.exception.AccountLockedException;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import br.com.fiap.pos.tech_challenge.core.security.TokenBlacklistService;
import br.com.fiap.pos.tech_challenge.core.security.TokenUtility;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock TokenUtility tokenUtility;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock AuthenticationConfiguration configuration;
    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthenticationService sut;

    // ---- authenticate ----

    @Test
    void authenticate_returnsTokenOnSuccess() throws Exception {
        User user = activeUser("operador");
        LoginDTO dto = new LoginDTO("operador", "senha123");

        UserDetailsImpl impl = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(impl);

        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(auth);
        when(configuration.getAuthenticationManager()).thenReturn(manager);
        when(userRepository.findByLogin("operador")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(tokenUtility.generate(impl)).thenReturn(mock(TokenDTO.class));

        TokenDTO result = sut.authenticate(dto);

        assertThat(result).isNotNull();
        verify(userService).updateLastLogin(user.getId());
        verify(auditLogService).register(any(), eq(user), eq("operador"), eq("200"), isNull());
    }

    @Test
    void authenticate_throwsWhenUserIsInactive() {
        User user = activeUser("operador");
        user.setActive(false);
        LoginDTO dto = new LoginDTO("operador", "senha123");

        when(userRepository.findByLogin("operador")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sut.authenticate(dto))
                .isInstanceOf(AccountInactiveException.class);

        verify(auditLogService).register(any(), eq(user), eq("operador"), eq("401"), any());
    }

    @Test
    void authenticate_throwsWhenAccountIsStillLocked() {
        User user = activeUser("operador");
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        LoginDTO dto = new LoginDTO("operador", "senha123");

        when(userRepository.findByLogin("operador")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sut.authenticate(dto))
                .isInstanceOf(AccountLockedException.class);

        verify(auditLogService).register(any(), eq(user), eq("operador"), eq("423"), any());
    }

    @Test
    void authenticate_resetsLockAndContinuesWhenLockExpired() throws Exception {
        User user = activeUser("operador");
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setLoginFailedAttempts(5);
        LoginDTO dto = new LoginDTO("operador", "senha123");

        UserDetailsImpl impl = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(impl);

        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(auth);
        when(configuration.getAuthenticationManager()).thenReturn(manager);
        when(userRepository.findByLogin("operador")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(tokenUtility.generate(impl)).thenReturn(mock(TokenDTO.class));

        sut.authenticate(dto);

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLoginFailedAttempts()).isZero();
    }

    @Test
    void authenticate_handlesFailedAttemptWhenUserNotFound() throws Exception {
        LoginDTO dto = new LoginDTO("ghost", "senha123");

        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(configuration.getAuthenticationManager()).thenReturn(manager);
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.authenticate(dto))
                .isInstanceOf(CoreException.class);

        verify(auditLogService).register(any(), isNull(), eq("ghost"), eq("401"), any());
    }

    @Test
    void authenticate_incrementsFailedAttemptsOnBadCredentials() throws Exception {
        User user = activeUser("operador");
        user.setLoginFailedAttempts(0);
        LoginDTO dto = new LoginDTO("operador", "errada");

        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(configuration.getAuthenticationManager()).thenReturn(manager);
        when(userRepository.findByLogin("operador")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sut.authenticate(dto))
                .isInstanceOf(CoreException.class);

        assertThat(user.getLoginFailedAttempts()).isEqualTo(1);
        verify(userService).saveLoginState(user);
    }

    @Test
    void authenticate_locksAccountAfterMaxAttempts() throws Exception {
        User user = activeUser("operador");
        user.setLoginFailedAttempts(4);
        LoginDTO dto = new LoginDTO("operador", "errada");

        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(configuration.getAuthenticationManager()).thenReturn(manager);
        when(userRepository.findByLogin("operador")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> sut.authenticate(dto))
                .isInstanceOf(CoreException.class);

        assertThat(user.getLockedUntil()).isNotNull();
        verify(userService).saveLoginState(user);
    }

    // ---- signout ----

    @Test
    void signout_invalidatesTokenInBlacklist() {
        String rawToken = "Bearer abc.def.ghi";
        UserDetailsImpl user = mock(UserDetailsImpl.class);

        when(tokenUtility.getJti(rawToken)).thenReturn("jti-123");
        when(tokenUtility.getRemainingTtlSeconds(rawToken)).thenReturn(3600L);

        sut.signout(rawToken, user);

        verify(tokenBlacklistService).invalidate("jti-123", 3600L);
        verify(auditLogService).register(any(), eq(user), eq("jti-123"), eq("204"), isNull());
    }

    @Test
    void signout_continuesAndLogsAuditWhenBlacklistThrows() {
        String rawToken = "token";
        UserDetailsImpl user = mock(UserDetailsImpl.class);

        when(tokenUtility.getJti(rawToken)).thenReturn("jti-456");
        when(tokenUtility.getRemainingTtlSeconds(rawToken)).thenReturn(300L);
        doThrow(new RuntimeException("redis offline")).when(tokenBlacklistService).invalidate(any(), anyLong());

        sut.signout(rawToken, user);

        verify(auditLogService).register(any(), eq(user), eq("jti-456"), eq("204"), isNull());
    }

    // ---- validatePassword ----

    @Test
    void validatePassword_succeedsWhenPasswordMatches() {
        User user = new User();
        user.setLogin("atendente");
        user.setPassword("$2a$encoded");

        when(userRepository.findByLogin("atendente")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "$2a$encoded")).thenReturn(true);

        sut.validatePassword("atendente", "senha123");

        verify(auditLogService).register(any(), eq(user), eq("atendente"), eq("200"), isNull());
    }

    @Test
    void validatePassword_throwsWhenPasswordMismatch() {
        User user = new User();
        user.setLogin("atendente");
        user.setPassword("$2a$encoded");

        when(userRepository.findByLogin("atendente")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senhaerrada", "$2a$encoded")).thenReturn(false);

        assertThatThrownBy(() -> sut.validatePassword("atendente", "senhaerrada"))
                .isInstanceOf(CoreException.class);

        verify(auditLogService).register(any(), eq(user), eq("atendente"), eq("401"), isNull());
    }

    @Test
    void validatePassword_throwsWhenUserNotFound() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.validatePassword("ghost", "qualquer"))
                .isInstanceOf(CoreException.class);
    }

    // ---- helpers ----

    private User activeUser(String login) {
        User u = new User();
        u.setId(1L);
        u.setLogin(login);
        u.setPassword("$2a$encoded");
        u.setActive(true);
        u.setRole(UserRole.ATTENDANT);
        u.setLoginFailedAttempts(0);
        return u;
    }
}
