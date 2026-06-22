package br.com.fiap.pos.tech_challenge.core.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record UserDTO(Long id, String firstName, String lastName, String email, LocalDate birthday, String login,
                      String phone, LocalDateTime createdAt, LocalDateTime lastLogin) implements Serializable {

}
