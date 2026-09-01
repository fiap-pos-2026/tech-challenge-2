package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.web.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.web.mapper.UserMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.PasswordHasher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository repository;
    @Mock UserMapper mapper;
    @Mock br.com.fiap.pos.tech_challenge.core.application.AuditLogService auditLogService;
    @Mock PasswordHasher passwordHasher;
    @Mock CurrentActorPort currentActorPort;

    @InjectMocks UserService sut;

    @BeforeEach
    void setUpCurrentActor() {
        User admin = user("admin");
        admin.setId(999L);
        admin.setRole(UserRole.ADMIN);
        lenient().when(currentActorPort.currentUser()).thenReturn(Optional.of(admin));
    }

    // ---- loadUserByUsername ----

    @Test
    void loadUserByUsername_returnsUserDetailsWhenFound() {
        User user = user("atendente");
        when(repository.findByLogin("atendente")).thenReturn(Optional.of(user));

        UserDetails result = sut.loadUserByUsername("atendente");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("atendente");
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(repository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("unknown"))
                .isInstanceOf(CoreException.class);
    }

    // ---- findAll ----

    @Test
    void findAll_delegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of(user("a")));
        when(mapper.toDTOs(any())).thenReturn(List.of(userDTO()));

        assertThat(sut.findAll()).hasSize(1);
    }

    // ---- findByUuid ----

    @Test
    void findByUuid_returnsDTO() {
        UUID uuid = UUID.randomUUID();
        User user = user("a");
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(user));
        when(mapper.toDTO(user)).thenReturn(userDTO());

        assertThat(sut.findByUuid(uuid)).isNotNull();
    }

    @Test
    void findByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByUuid(uuid))
                .isInstanceOf(CoreException.class);
    }

    // ---- create / validate ----

    @Test
    void create_savesAndReturnsDTO() {
        CreateUserDTO dto = createDto("joao", "joao@mail.com");
        User entity = user("joao");
        UserDTO resp = userDTO();

        when(repository.existsByLoginIgnoreCase("joao")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("joao@mail.com")).thenReturn(false);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(resp);

        assertThat(sut.create(dto)).isEqualTo(resp);
    }

    @Test
    void create_throwsWhenLoginAlreadyExists() {
        CreateUserDTO dto = createDto("joao", "joao@mail.com");
        when(repository.existsByLoginIgnoreCase("joao")).thenReturn(true);

        assertThatThrownBy(() -> sut.create(dto))
                .isInstanceOf(CoreException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_throwsWhenEmailAlreadyExists() {
        CreateUserDTO dto = createDto("joao", "joao@mail.com");
        when(repository.existsByLoginIgnoreCase("joao")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("joao@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.create(dto))
                .isInstanceOf(CoreException.class);
    }

    // ---- deleteByUuid ----

    @Test
    void deleteByUuid_removesWhenExists() {
        UUID uuid = UUID.randomUUID();
        User target = user("atendente");
        target.setId(1L);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(target));

        sut.deleteByUuid(uuid);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.deleteByUuid(uuid))
                .isInstanceOf(CoreException.class);
    }

    // ---- update ----

    @Test
    void update_appliesChangesAndReturns() {
        UUID uuid = UUID.randomUUID();
        User current = user("joao");
        current.setId(1L);
        UpdateUserDTO dto = updateDto("joao", "novo@mail.com");
        UserDTO resp = userDTO();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(current));
        when(repository.existsByEmailIgnoreCase("novo@mail.com")).thenReturn(false);
        when(mapper.fullUpdate(dto, current)).thenReturn(current);
        when(repository.save(current)).thenReturn(current);
        when(mapper.toDTO(current)).thenReturn(resp);

        assertThat(sut.update(uuid, dto)).isEqualTo(resp);
    }

    @Test
    void update_throwsWhenLoginTakenByOther() {
        UUID uuid = UUID.randomUUID();
        User current = user("joao");
        current.setLogin("joao");
        current.setId(1L);
        UpdateUserDTO dto = updateDto("pedro", "joao@mail.com");

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(current));
        when(repository.existsByLoginIgnoreCase("pedro")).thenReturn(true);

        assertThatThrownBy(() -> sut.update(uuid, dto))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void update_throwsWhenEmailTakenByOther() {
        UUID uuid = UUID.randomUUID();
        User current = user("joao");
        current.setEmail("joao@antigo.com");
        current.setId(1L);
        UpdateUserDTO dto = updateDto("joao", "outro@mail.com");

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(current));
        when(repository.existsByEmailIgnoreCase("outro@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.update(uuid, dto))
                .isInstanceOf(CoreException.class);
    }

    // ---- changePassword ----

    @Test
    void changePassword_succeedsAndClearsLockout() {
        User u = user("joao");
        u.setId(1L);
        u.setLoginFailedAttempts(2);
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordHasher.matches("senhaAtual", "hash")).thenReturn(true);
        when(passwordHasher.matches("novaSenha@1", "hash")).thenReturn(false);
        when(passwordHasher.hash("novaSenha@1")).thenReturn("newHash");

        sut.changePassword(1L, new br.com.fiap.pos.tech_challenge.core.web.dto.ChangePasswordDTO("senhaAtual", "novaSenha@1"));

        assertThat(u.getLoginFailedAttempts()).isZero();
        assertThat(u.getLockedUntil()).isNull();
        assertThat(u.isForceChangePassword()).isFalse();
        verify(repository).save(u);
        verify(auditLogService).register(any(), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    void changePassword_throwsWhenCurrentPasswordWrongAndIncrementsAttempts() {
        User u = user("joao");
        u.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordHasher.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() ->
                sut.changePassword(1L, new br.com.fiap.pos.tech_challenge.core.web.dto.ChangePasswordDTO("errada", "NovaSenha@1")))
                .isInstanceOf(CoreException.class);

        assertThat(u.getLoginFailedAttempts()).isEqualTo(1);
        verify(auditLogService).register(any(), any(), any(), eq("401"), any());
    }

    @Test
    void changePassword_throwsWhenNewPasswordSameAsCurrent() {
        User u = user("joao");
        u.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        // both calls use same args → single stub covers current-matches + new-same-as-current check
        when(passwordHasher.matches("senhaAtual", "hash")).thenReturn(true);

        assertThatThrownBy(() ->
                sut.changePassword(1L, new br.com.fiap.pos.tech_challenge.core.web.dto.ChangePasswordDTO("senhaAtual", "senhaAtual")))
                .isInstanceOf(CoreException.class);
    }

    // ---- deleteByUuid — last admin guard ----

    @Test
    void deleteByUuid_throwsWhenDeletingLastAdmin() {
        UUID uuid = UUID.randomUUID();
        User target = user("admin2");
        target.setId(2L);
        target.setRole(UserRole.ADMIN);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(target));
        when(repository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> sut.deleteByUuid(uuid))
                .isInstanceOf(CoreException.class);
        verify(repository, never()).deleteById(any());
    }

    // ---- changePassword — lockout edge cases ----

    @Test
    void changePassword_throwsWhenAccountIsLocked() {
        User u = user("joao");
        u.setId(1L);
        u.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(repository.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() ->
                sut.changePassword(1L, new br.com.fiap.pos.tech_challenge.core.web.dto.ChangePasswordDTO("senhaAtual", "novaSenha@1")))
                .isInstanceOf(CoreException.class);
        verify(passwordHasher, never()).matches(any(), any());
    }

    @Test
    void changePassword_locksAccountAfterMaxAttempts() {
        User u = user("joao");
        u.setId(1L);
        u.setLoginFailedAttempts(4);
        when(repository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordHasher.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() ->
                sut.changePassword(1L, new br.com.fiap.pos.tech_challenge.core.web.dto.ChangePasswordDTO("errada", "nova@1")))
                .isInstanceOf(CoreException.class);

        assertThat(u.getLockedUntil()).isNotNull();
        assertThat(u.getLoginFailedAttempts()).isEqualTo(5);
        verify(repository).save(u);
    }

    // ---- getLoggedUser ----

    @Test
    void getLoggedUser_returnsCurrentUserDTO() {
        User entity = user("admin");
        entity.setId(999L);
        when(repository.findById(999L)).thenReturn(Optional.of(entity));
        when(mapper.toDTO(entity)).thenReturn(userDTO());

        assertThat(sut.getLoggedUser()).isNotNull();
    }

    @Test
    void getLoggedUser_throwsWhenUserNotFoundInRepository() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getLoggedUser())
                .isInstanceOf(CoreException.class);
    }

    // ---- update — role change audit + self-role guard ----

    @Test
    void update_logsAuditWhenRoleChanges() {
        UUID uuid = UUID.randomUUID();
        User current = user("mecanico");
        current.setId(1L);
        current.setRole(UserRole.MECHANIC);
        current.setEmail("mecanico@mail.com");
        UpdateUserDTO dto = new UpdateUserDTO("M", "S", "mecanico@mail.com", LocalDate.of(1990, 1, 1), "mecanico", "senha123", "11999999999", UserRole.ATTENDANT);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(current));
        when(mapper.fullUpdate(dto, current)).thenReturn(current);
        when(repository.save(current)).thenReturn(current);
        when(mapper.toDTO(current)).thenReturn(userDTO());

        sut.update(uuid, dto);

        verify(auditLogService).register(any(), any(), eq("mecanico"), eq("SUCCESS"), any());
    }

    @Test
    void update_throwsWhenAdminTriesToChangeOwnRole() {
        UUID uuid = UUID.randomUUID();
        User current = user("admin");
        current.setId(999L);
        current.setRole(UserRole.ADMIN);
        UpdateUserDTO dto = new UpdateUserDTO("A", "S", "admin@mail.com", LocalDate.of(1990, 1, 1), "admin", "senha", "11999999999", UserRole.MECHANIC);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> sut.update(uuid, dto))
                .isInstanceOf(CoreException.class);
        verify(repository, never()).save(any());
    }

    // ---- findByLogin ----

    @Test
    void findByLogin_returnsUserWhenFound() {
        User user = user("atendente");
        when(repository.findByLogin("atendente")).thenReturn(Optional.of(user));

        assertThat(sut.findByLogin("atendente")).isSameAs(user);
    }

    @Test
    void findByLogin_throwsWhenNotFound() {
        when(repository.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByLogin("ghost"))
                .isInstanceOf(CoreException.class);
    }

    // ---- saveLoginState / updateLastLogin ----

    @Test
    void saveLoginState_delegatesToRepository() {
        User user = user("x");
        sut.saveLoginState(user);
        verify(repository).save(user);
    }

    @Test
    void updateLastLogin_delegatesToRepository() {
        sut.updateLastLogin(1L);
        verify(repository).updateLastLogin(1L);
    }

    // ---- helpers ----
    private User user(String login) {
        User u = new User();
        u.setLogin(login);
        u.setPassword("hash");
        return u;
    }

    private UserDTO userDTO() {
        return mock(UserDTO.class);
    }

    private CreateUserDTO createDto(String login, String email) {
        return new CreateUserDTO("João", "Silva", email, LocalDate.of(1990, 1, 1), login, "senha123", "11999999999", null);
    }

    private UpdateUserDTO updateDto(String login, String email) {
        return new UpdateUserDTO("João", "Silva", email, LocalDate.of(1990, 1, 1), login, "senha123", "11999999999", null);
    }
}
