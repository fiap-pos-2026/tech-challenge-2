package br.com.fiap.pos.tech_challenge.core.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VehicleResponse(
        UUID uuid,
        String licensePlate,
        String make,
        String model,
        int year,
        UUID customerUuid
) implements Serializable {
}
