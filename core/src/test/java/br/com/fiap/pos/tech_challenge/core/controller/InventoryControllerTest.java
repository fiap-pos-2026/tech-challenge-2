package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.ManualAdjustmentRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ReplenishmentRequest;
import br.com.fiap.pos.tech_challenge.core.domain.StockMovement;
import br.com.fiap.pos.tech_challenge.core.mapper.StockMovementMapper;
import br.com.fiap.pos.tech_challenge.core.service.StockService;
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
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock StockService stockService;
    @Mock StockMovementMapper movementMapper;

    @InjectMocks InventoryController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("POST /api/inventory/products/{uuid}/replenishment")
    class Replenish {
        @Test
        void replenish_returns204() throws Exception {
            UUID uuid = UUID.randomUUID();
            ReplenishmentRequest request = new ReplenishmentRequest(new BigDecimal("10.0"));

            mockMvc.perform(post("/api/inventory/products/{uuid}/replenishment", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/manual-adjustment")
    class ManualAdjustment {
        @Test
        void registerManualAdjustment_returns204() throws Exception {
            ManualAdjustmentRequest request = new ManualAdjustmentRequest(UUID.randomUUID(), new BigDecimal("2"), "descarte");

            mockMvc.perform(post("/api/inventory/manual-adjustment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/inventory/movements")
    class ListMovements {
        @Test
        void listMovements_withoutFilter_returns200() throws Exception {
            PageImpl<StockMovement> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(stockService.listAllMovements(any())).thenReturn(page);

            mockMvc.perform(get("/api/inventory/movements"))
                    .andExpect(status().isOk());
        }

        @Test
        void listMovements_withProductId_returns200() throws Exception {
            PageImpl<StockMovement> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(stockService.listMovementsByProduct(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/inventory/movements").param("productId", "1"))
                    .andExpect(status().isOk());
        }
    }
}