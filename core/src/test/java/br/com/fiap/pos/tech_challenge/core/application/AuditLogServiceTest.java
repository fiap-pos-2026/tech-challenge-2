package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.domain.model.SecurityAuditLog;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.application.port.out.SecurityAuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock SecurityAuditLogRepository repository;

    @InjectMocks AuditLogService sut;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("persiste log com todos os campos preenchidos")
        void register_persistsLogWithAllFields() {
            User user = new User();
            user.setLogin("operador");

            sut.register(AuditEventType.LOGIN_SUCCESS, user, "operador", "200", "detalhe");

            ArgumentCaptor<SecurityAuditLog> captor = ArgumentCaptor.forClass(SecurityAuditLog.class);
            verify(repository).save(captor.capture());

            SecurityAuditLog saved = captor.getValue();
            assertThat(saved.getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getAttemptIdentifier()).isEqualTo("operador");
            assertThat(saved.getResult()).isEqualTo("200");
            assertThat(saved.getDetails()).isEqualTo("detalhe");
        }

        @Test
        @DisplayName("persiste log sem usuario (tentativa com usuario invalido)")
        void register_persistsLogWithNullUser() {
            sut.register(AuditEventType.LOGIN_FAILED, null, "ghost", "401", "Usuário não encontrado");

            ArgumentCaptor<SecurityAuditLog> captor = ArgumentCaptor.forClass(SecurityAuditLog.class);
            verify(repository).save(captor.capture());

            SecurityAuditLog saved = captor.getValue();
            assertThat(saved.getUser()).isNull();
            assertThat(saved.getAttemptIdentifier()).isEqualTo("ghost");
            assertThat(saved.getResult()).isEqualTo("401");
        }

        @Test
        @DisplayName("persiste log sem detalhes")
        void register_persistsLogWithNullDetails() {
            User user = new User();

            sut.register(AuditEventType.LOGOUT_SUCCESS, user, "jti-abc", "204", null);

            ArgumentCaptor<SecurityAuditLog> captor = ArgumentCaptor.forClass(SecurityAuditLog.class);
            verify(repository).save(captor.capture());

            assertThat(captor.getValue().getDetails()).isNull();
        }
    }
}