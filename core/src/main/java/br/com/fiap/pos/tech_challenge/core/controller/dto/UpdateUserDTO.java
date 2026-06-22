package br.com.fiap.pos.tech_challenge.core.controller.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateUserDTO(@NotBlank String firstName, @NotBlank String lastName, @NotBlank String email,
                            @NotNull LocalDate birthday, @NotBlank String login, @NotBlank String password,
                            @NotBlank String phone) {

    @Override
    public String login() {
        if (this.login == null) {
            return null;
        }
        return this.login.toLowerCase();
    }

    @Override
    public String email() {
        if (this.email == null) {
            return null;
        }
        return this.email.toLowerCase();
    }
}
