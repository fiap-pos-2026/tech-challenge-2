package br.com.fiap.pos.tech_challenge.core.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * @author johncgo
 * @since 2026-06-25
 */
public record CreateServiceRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice,
        @NotNull @Positive Integer estimatedDurationMinutes
) {
}
