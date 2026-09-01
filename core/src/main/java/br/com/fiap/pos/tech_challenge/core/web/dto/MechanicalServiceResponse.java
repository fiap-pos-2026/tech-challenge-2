package br.com.fiap.pos.tech_challenge.core.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-25
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record MechanicalServiceResponse(
        UUID uuid,
        String name,
        String description,
        BigDecimal basePrice,
        int estimatedDurationMinutes
) implements Serializable {
}
