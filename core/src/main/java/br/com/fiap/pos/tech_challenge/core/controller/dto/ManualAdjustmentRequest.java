package br.com.fiap.pos.tech_challenge.core.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
public record ManualAdjustmentRequest(
        @NotNull UUID productUuid,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotBlank String reason
) {
}
