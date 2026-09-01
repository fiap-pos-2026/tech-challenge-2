package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringUserDetailsServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks SpringUserDetailsService sut;

    @Test
    void loadUserByUsername_returnsUserDetailsWhenFound() {
        User user = new User();
        user.setLogin("atendente");
        user.setPassword("hash");
        when(userRepository.findByLogin("atendente")).thenReturn(Optional.of(user));

        UserDetails result = sut.loadUserByUsername("atendente");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("atendente");
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("unknown"))
                .isInstanceOf(CoreException.class);
    }
}
