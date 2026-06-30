package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateProductRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ProductResponse;
import br.com.fiap.pos.tech_challenge.core.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.enums.ProductType;
import br.com.fiap.pos.tech_challenge.core.service.ProductService;
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
class ProductControllerTest {

    @Mock ProductService service;

    @InjectMocks ProductController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private ProductResponse stubResponse() {
        return new ProductResponse(UUID.randomUUID(), "Filtro", ProductType.PART, MeasurementUnit.UNIT, new BigDecimal("25.00"), new BigDecimal("10"), true);
    }

    @Nested
    @DisplayName("POST /api/inventory/products")
    class Create {
        @Test
        void create_returns201() throws Exception {
            CreateProductRequest request = new CreateProductRequest("Filtro", null, ProductType.PART, MeasurementUnit.UNIT, new BigDecimal("25.00"), BigDecimal.ZERO, true);
            when(service.create(any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/inventory/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/products")
    class FindAll {
        @Test
        void findAll_returns200() throws Exception {
            when(service.findAll(any())).thenReturn(new PageImpl<>(List.of(stubResponse()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/inventory/products"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/products/{uuid}")
    class FindByUuid {
        @Test
        void findByUuid_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.findByUuid(uuid)).thenReturn(stubResponse());

            mockMvc.perform(get("/api/inventory/products/{uuid}", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/inventory/products/{uuid}")
    class Update {
        @Test
        void update_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            CreateProductRequest request = new CreateProductRequest("Filtro Atualizado", null, ProductType.PART, MeasurementUnit.UNIT, new BigDecimal("30.00"), BigDecimal.ZERO, true);
            when(service.update(any(), any())).thenReturn(stubResponse());

            mockMvc.perform(put("/api/inventory/products/{uuid}", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/inventory/products/{uuid}")
    class Delete {
        @Test
        void delete_returns204() throws Exception {
            UUID uuid = UUID.randomUUID();

            mockMvc.perform(delete("/api/inventory/products/{uuid}", uuid))
                    .andExpect(status().isNoContent());

            verify(service).delete(uuid);
        }
    }
}