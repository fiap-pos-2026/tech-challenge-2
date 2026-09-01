package br.com.fiap.pos.tech_challenge.core.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record AddServiceRequest(
        @NotNull UUID mechanicalServiceUuid
) {
}
