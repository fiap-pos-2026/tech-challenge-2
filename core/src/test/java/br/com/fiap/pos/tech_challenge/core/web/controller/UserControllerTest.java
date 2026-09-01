package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.web.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.application.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService service;

    @InjectMocks UserController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private UserDTO stubUserDTO() {
        return new UserDTO(UUID.randomUUID(), "Joao", "Silva", "joao@mail.com", LocalDate.of(1990, 1, 1), "joao", "11999999999", null, null);
    }

    @Nested
    @DisplayName("GET /api/users")
    class FindAll {
        @Test
        void findAll_returns200() throws Exception {
            when(service.findAll()).thenReturn(List.of(stubUserDTO()));

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{uuid}")
    class FindByUuid {
        @Test
        void findByUuid_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.findByUuid(uuid)).thenReturn(stubUserDTO());

            mockMvc.perform(get("/api/users/{uuid}", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    class Create {
        @Test
        void create_returns201() throws Exception {
            CreateUserDTO dto = new CreateUserDTO("Joao", "Silva", "joao@mail.com", LocalDate.of(1990, 1, 1), "joao", "senha123", "11999999999", null);
            when(service.create(any())).thenReturn(stubUserDTO());

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{uuid}")
    class Delete {
        @Test
        void delete_returns204() throws Exception {
            UUID uuid = UUID.randomUUID();

            mockMvc.perform(delete("/api/users/{uuid}", uuid))
                    .andExpect(status().isNoContent());

            verify(service).deleteByUuid(uuid);
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{uuid}")
    class Update {
        @Test
        void update_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            UpdateUserDTO dto = new UpdateUserDTO("Joao", "Silva", "joao@mail.com", LocalDate.of(1990, 1, 1), "joao", "senha123", "11999999999", null);
            when(service.update(any(), any())).thenReturn(stubUserDTO());

            mockMvc.perform(put("/api/users/{uuid}", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }
    }
}