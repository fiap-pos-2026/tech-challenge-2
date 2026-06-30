package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.CustomerResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.service.CustomerService;
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
class CustomerControllerTest {

    @Mock CustomerService service;

    @InjectMocks CustomerController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private CustomerResponse stubResponse() {
        return new CustomerResponse(UUID.randomUUID(), DocumentType.CPF, "***", "Cliente Teste", "cliente@mail.com", "11999999999", null);
    }

    @Nested
    @DisplayName("POST /api/customers")
    class Register {
        @Test
        void register_returns201() throws Exception {
            CreateCustomerRequest request = new CreateCustomerRequest(DocumentType.CPF, "52998224725", "Cliente Teste", "cliente@mail.com", "11999999999");
            when(service.register(any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/customers")
    class FindByDocument {
        @Test
        void findByDocument_returns200() throws Exception {
            when(service.findByDocument("52998224725")).thenReturn(stubResponse());

            mockMvc.perform(get("/api/customers").param("document", "52998224725"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/customers/{uuid}")
    class FindByUuid {
        @Test
        void findByUuid_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.findByUuid(uuid)).thenReturn(stubResponse());

            mockMvc.perform(get("/api/customers/{uuid}", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/customers/{uuid}")
    class Update {
        @Test
        void update_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            UpdateCustomerRequest request = new UpdateCustomerRequest("Cliente Novo", "novo@mail.com", "11999999999");
            when(service.update(any(), any())).thenReturn(stubResponse());

            mockMvc.perform(put("/api/customers/{uuid}", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customers/{uuid}")
    class Delete {
        @Test
        void delete_returns204() throws Exception {
            UUID uuid = UUID.randomUUID();

            mockMvc.perform(delete("/api/customers/{uuid}", uuid))
                    .andExpect(status().isNoContent());

            verify(service).delete(uuid);
        }
    }

    @Nested
    @DisplayName("GET /api/customers/{uuid}/document")
    class FindFullDocument {
        @Test
        void findFullDocument_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.findFullDocument(uuid)).thenReturn("52998224725");

            mockMvc.perform(get("/api/customers/{uuid}/document", uuid))
                    .andExpect(status().isOk());
        }
    }
}