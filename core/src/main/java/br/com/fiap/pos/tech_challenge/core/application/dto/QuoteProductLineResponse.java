package br.com.fiap.pos.tech_challenge.core.application.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record QuoteProductLineResponse(
        UUID productUuid,
        String nameSnapshot,
        BigDecimal unitPriceSnapshot,
        BigDecimal quantity,
        MeasurementUnit measurementUnit,
        boolean unbudgeted
) implements Serializable {
}
