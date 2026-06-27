package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.mapper.UserMapper;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @InjectMocks UserService sut;

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

    // ---- findById ----

    @Test
    void findById_returnsDTO() {
        User user = user("a");
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(mapper.toDTO(user)).thenReturn(userDTO());

        assertThat(sut.findById(1L)).isNotNull();
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findById(99L))
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

    // ---- deleteById ----

    @Test
    void deleteById_removesWhenExists() {
        when(repository.existsById(1L)).thenReturn(true);

        sut.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteById_throwsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sut.deleteById(99L))
                .isInstanceOf(CoreException.class);
    }

    // ---- update ----

    @Test
    void update_appliesChangesAndReturns() {
        Long id = 1L;
        User current = user("joao");
        // use different email so existsByEmailIgnoreCase is checked
        UpdateUserDTO dto = updateDto("joao", "novo@mail.com");
        UserDTO resp = userDTO();

        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.existsByEmailIgnoreCase("novo@mail.com")).thenReturn(false);
        when(mapper.fullUpdate(dto, current)).thenReturn(current);
        when(repository.save(current)).thenReturn(current);
        when(mapper.toDTO(current)).thenReturn(resp);

        assertThat(sut.update(id, dto)).isEqualTo(resp);
    }

    @Test
    void update_throwsWhenLoginTakenByOther() {
        Long id = 1L;
        User current = user("joao");
        current.setLogin("joao");
        UpdateUserDTO dto = updateDto("pedro", "joao@mail.com");

        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.existsByLoginIgnoreCase("pedro")).thenReturn(true);

        assertThatThrownBy(() -> sut.update(id, dto))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void update_throwsWhenEmailTakenByOther() {
        Long id = 1L;
        User current = user("joao");
        current.setEmail("joao@antigo.com");
        UpdateUserDTO dto = updateDto("joao", "outro@mail.com");

        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.existsByEmailIgnoreCase("outro@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.update(id, dto))
                .isInstanceOf(CoreException.class);
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
        return new CreateUserDTO("João", "Silva", email, LocalDate.of(1990, 1, 1), login, "senha123", "11999999999");
    }

    private UpdateUserDTO updateDto(String login, String email) {
        return new UpdateUserDTO("João", "Silva", email, LocalDate.of(1990, 1, 1), login, "senha123", "11999999999");
    }
}
