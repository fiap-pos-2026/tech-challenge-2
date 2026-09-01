package br.com.fiap.pos.tech_challenge.core.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserDTO(UUID uuid, String firstName, String lastName, String email, LocalDate birthday, String login,
                      String phone, LocalDateTime createdAt, LocalDateTime lastLogin) implements Serializable {

}
