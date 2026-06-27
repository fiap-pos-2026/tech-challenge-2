package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.fiap.pos.tech_challenge.core.security.TokenUtility;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock TokenUtility tokenUtility;
    @Mock AuthenticationConfiguration configuration;
    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthenticationService sut;

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
}
