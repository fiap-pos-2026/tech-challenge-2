package br.com.fiap.pos.tech_challenge.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @author johncgo
 * @since 2026-06-27
 */
public record ChangePasswordDTO(

        @NotBlank
        String senhaAtual,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
                message = "error.password_policy"
        )
        String novaSenha
) {}
