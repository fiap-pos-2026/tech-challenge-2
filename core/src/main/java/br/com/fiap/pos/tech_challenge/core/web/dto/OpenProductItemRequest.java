package br.com.fiap.pos.tech_challenge.core.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-08-23
 */
public record OpenProductItemRequest(
        @NotNull UUID productUuid,
        // SPEC_DEVIATION: design.md especifica Integer quantity
        // Reason: QuoteProductLine.quantity e AddProductRequest usam BigDecimal (produtos têm unidade de medida fracionária)
        @NotNull @DecimalMin("0.001") BigDecimal quantity
) {
}
