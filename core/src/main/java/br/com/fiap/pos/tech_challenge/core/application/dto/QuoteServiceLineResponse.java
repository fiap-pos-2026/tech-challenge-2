package br.com.fiap.pos.tech_challenge.core.application.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record QuoteServiceLineResponse(
        UUID mechanicalServiceUuid,
        String nameSnapshot,
        BigDecimal priceSnapshot,
        int estimatedDurationMinutes
) implements Serializable {
}
