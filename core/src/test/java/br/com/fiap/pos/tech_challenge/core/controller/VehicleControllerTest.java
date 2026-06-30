package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.VehicleResponse;
import br.com.fiap.pos.tech_challenge.core.service.VehicleService;
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
class VehicleControllerTest {

    @Mock VehicleService service;

    @InjectMocks VehicleController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private VehicleResponse stubResponse() {
        return new VehicleResponse(UUID.randomUUID(), "ABC1234", "Toyota", "Corolla", 2020, UUID.randomUUID());
    }

    @Nested
    @DisplayName("POST /api/vehicles")
    class Register {
        @Test
        void register_returns201() throws Exception {
            CreateVehicleRequest request = new CreateVehicleRequest("ABC1D23", "Toyota", "Corolla", 2020, UUID.randomUUID());
            when(service.register(any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/vehicles")
    class FindByLicensePlate {
        @Test
        void findByLicensePlate_returns200() throws Exception {
            when(service.findByLicensePlate("ABC1234")).thenReturn(stubResponse());

            mockMvc.perform(get("/api/vehicles").param("licensePlate", "ABC1234"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/vehicles/{uuid}")
    class Update {
        @Test
        void update_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            UpdateVehicleRequest request = new UpdateVehicleRequest("Honda", "Civic", 2021);
            when(service.update(any(), any())).thenReturn(stubResponse());

            mockMvc.perform(put("/api/vehicles/{uuid}", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/vehicles/{uuid}")
    class Delete {
        @Test
        void delete_returns204() throws Exception {
            UUID uuid = UUID.randomUUID();

            mockMvc.perform(delete("/api/vehicles/{uuid}", uuid))
                    .andExpect(status().isNoContent());

            verify(service).delete(uuid);
        }
    }

    @Nested
    @DisplayName("GET /api/vehicles/{uuid}")
    class FindByUuid {
        @Test
        void findByUuid_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.findByUuid(uuid)).thenReturn(stubResponse());

            mockMvc.perform(get("/api/vehicles/{uuid}", uuid))
                    .andExpect(status().isOk());
        }
    }
}