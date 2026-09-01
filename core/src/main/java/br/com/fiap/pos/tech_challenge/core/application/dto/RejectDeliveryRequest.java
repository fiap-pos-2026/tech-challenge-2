package br.com.fiap.pos.tech_challenge.core.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record RejectDeliveryRequest(
        @NotBlank String token,
        @NotBlank String customerDocument,
        @NotBlank String reason
) {
}
