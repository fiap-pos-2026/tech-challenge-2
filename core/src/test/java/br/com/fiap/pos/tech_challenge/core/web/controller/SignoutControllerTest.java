package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.application.AuthenticationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class SignoutControllerTest {

    @Mock AuthenticationService service;

    @InjectMocks SignoutController controller;

    MockMvc mockMvc;

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
    @DisplayName("POST /api/signout")
    class Signout {
        @Test
        void signout_returns204() throws Exception {
            mockMvc.perform(post("/api/signout")
                            .header("Authorization", "Bearer sometoken"))
                    .andExpect(status().isNoContent());
        }
    }
}