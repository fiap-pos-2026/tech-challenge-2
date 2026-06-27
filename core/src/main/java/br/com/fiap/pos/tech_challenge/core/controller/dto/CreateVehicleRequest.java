package br.com.fiap.pos.tech_challenge.core.controller.dto;

import br.com.fiap.pos.tech_challenge.core.validation.ValidLicensePlate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record CreateVehicleRequest(
        @NotBlank @ValidLicensePlate String licensePlate,
        @NotBlank String make,
        @NotBlank String model,
        @NotNull @Min(1900) @Max(2100) Integer year,
        @NotNull UUID customerUuid
) {
}
