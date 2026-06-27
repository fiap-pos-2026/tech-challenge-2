package br.com.fiap.pos.tech_challenge.core.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record OpenServiceOrderRequest(
        @NotNull UUID customerUuid,
        @NotNull UUID vehicleUuid,
        @NotBlank @Size(max = 2000) String customerComplaint
) {
}
