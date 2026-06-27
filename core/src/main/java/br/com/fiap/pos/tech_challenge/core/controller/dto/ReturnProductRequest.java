package br.com.fiap.pos.tech_challenge.core.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record ReturnProductRequest(
        @NotBlank String password,
        @NotNull UUID productLineUuid
) {
}
