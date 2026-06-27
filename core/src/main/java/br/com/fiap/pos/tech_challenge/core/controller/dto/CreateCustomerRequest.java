package br.com.fiap.pos.tech_challenge.core.controller.dto;

import br.com.fiap.pos.tech_challenge.core.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.validation.ValidTaxId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record CreateCustomerRequest(
        @NotNull DocumentType documentType,
        @NotBlank @ValidTaxId String document,
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone
) {
}
