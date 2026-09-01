package br.com.fiap.pos.tech_challenge.core.application.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ServiceOrderResponse(
        UUID uuid,
        ServiceOrderStatus status,
        String customerComplaint,
        QuoteResponse quote,
        LocalDateTime createdAt
) implements Serializable {
}
