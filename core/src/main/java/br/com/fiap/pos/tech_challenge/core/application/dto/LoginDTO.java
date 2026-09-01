package br.com.fiap.pos.tech_challenge.core.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    @Schema(
            description = "Login do usuário.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String login;

    @Schema(
            description = "Senha do usuário.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String password;
}
