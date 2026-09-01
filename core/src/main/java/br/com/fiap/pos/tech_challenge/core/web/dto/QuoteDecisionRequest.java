package br.com.fiap.pos.tech_challenge.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record QuoteDecisionRequest(
        @NotBlank String token,
        @NotBlank String customerDocument,
        @NotNull QuoteDecisionType decision
) {
}
