package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.ChangePasswordDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock UserService userService;

    @InjectMocks ProfileController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setLogin("user");
        UserDetailsImpl principal = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/profile")
    class GetProfile {
        @Test
        void getProfile_returns200() throws Exception {
            UserDTO dto = new UserDTO(UUID.randomUUID(), "Joao", "Silva", "j@mail.com", LocalDate.of(1990, 1, 1), "user", "11999999999", null, null);
            when(userService.getLoggedUser()).thenReturn(dto);

            mockMvc.perform(get("/api/profile"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/profile/password")
    class ChangePassword {
        @Test
        void changePassword_returns204() throws Exception {
            ChangePasswordDTO dto = new ChangePasswordDTO("senhaAtual", "NovaSenha@1");

            mockMvc.perform(put("/api/profile/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNoContent());
        }
    }
}