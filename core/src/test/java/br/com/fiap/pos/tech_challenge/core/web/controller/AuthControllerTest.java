package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.web.dto.LoginDTO;
import br.com.fiap.pos.tech_challenge.core.application.AuthenticationService;
import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthenticationService service;

    @InjectMocks AuthController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("POST /api/signin")
    class SignIn {
        @Test
        void signIn_returns200() throws Exception {
            LoginDTO dto = new LoginDTO("operador", "senha123");
            when(service.authenticate(any())).thenReturn(new TokenDTO("tok", "now", "later"));

            mockMvc.perform(post("/api/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }
    }
}