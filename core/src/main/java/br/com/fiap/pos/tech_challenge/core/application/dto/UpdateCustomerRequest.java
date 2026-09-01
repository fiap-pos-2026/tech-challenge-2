package br.com.fiap.pos.tech_challenge.core.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
public record UpdateCustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone
) {}
