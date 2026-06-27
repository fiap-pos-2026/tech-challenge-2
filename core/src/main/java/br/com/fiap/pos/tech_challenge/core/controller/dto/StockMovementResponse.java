package br.com.fiap.pos.tech_challenge.core.controller.dto;

import br.com.fiap.pos.tech_challenge.core.enums.MovementType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StockMovementResponse(
        UUID uuid,
        MovementType type,
        BigDecimal quantity,
        UUID serviceOrderUuid,
        LocalDateTime createdAt
) implements Serializable {
}
