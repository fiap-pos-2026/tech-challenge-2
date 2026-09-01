package br.com.fiap.pos.tech_challenge.core.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
public record UpdateVehicleRequest(
        @NotBlank String make,
        @NotBlank String model,
        @NotNull @Min(1900) @Max(2100) Integer year
) {}
