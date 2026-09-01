package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.application.dto.CreateServiceRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.MechanicalServiceResponse;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceAvgDurationResponse;
import br.com.fiap.pos.tech_challenge.core.application.MechanicalServiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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
class MechanicalServiceControllerTest {

    @Mock MechanicalServiceService service;

    @InjectMocks MechanicalServiceController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private MechanicalServiceResponse stubResponse() {
        return new MechanicalServiceResponse(UUID.randomUUID(), "Troca de oleo", null, new BigDecimal("150.00"), 60);
    }

    @Nested
    @DisplayName("POST /api/catalog/services")
    class Create {
        @Test
        void create_returns201() throws Exception {
            CreateServiceRequest request = new CreateServiceRequest("Troca de oleo", null, new BigDecimal("150.00"), 60);
            when(service.create(any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/catalog/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/catalog/services")
    class FindAll {
        @Test
        void findAll_returns200() throws Exception {
            when(service.findAll(any())).thenReturn(new PageImpl<>(List.of(stubResponse()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/catalog/services"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/catalog/services/{uuid}")
    class FindByUuid {
        @Test
        void findByUuid_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.findByUuid(uuid)).thenReturn(stubResponse());

            mockMvc.perform(get("/api/catalog/services/{uuid}", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/catalog/services/{uuid}")
    class Update {
        @Test
        void update_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            CreateServiceRequest request = new CreateServiceRequest("Alinhamento", null, new BigDecimal("80.00"), 45);
            when(service.update(any(), any())).thenReturn(stubResponse());

            mockMvc.perform(put("/api/catalog/services/{uuid}", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/catalog/services/avg-duration")
    class AvgDuration {
        @Test
        void avgDuration_returns200() throws Exception {
            when(service.findAvgDurations()).thenReturn(List.of(
                    new ServiceAvgDurationResponse(UUID.randomUUID(), "Troca de oleo", 60.0, 5L)));

            mockMvc.perform(get("/api/catalog/services/avg-duration"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/catalog/services/{uuid}")
    class Delete {
        @Test
        void delete_returns204() throws Exception {
            UUID uuid = UUID.randomUUID();

            mockMvc.perform(delete("/api/catalog/services/{uuid}", uuid))
                    .andExpect(status().isNoContent());

            verify(service).delete(uuid);
        }
    }
}