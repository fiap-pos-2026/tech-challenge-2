package br.com.fiap.pos.tech_challenge.core.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record QuoteResponse(
        UUID uuid,
        BigDecimal totalAmount,
        Instant approvedAt,
        List<QuoteServiceLineResponse> serviceLines,
        List<QuoteProductLineResponse> productLines
) implements Serializable {
}
