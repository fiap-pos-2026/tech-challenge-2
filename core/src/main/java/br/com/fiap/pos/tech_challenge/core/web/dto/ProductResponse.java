package br.com.fiap.pos.tech_challenge.core.web.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ProductType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProductResponse(
        UUID uuid,
        String name,
        ProductType type,
        MeasurementUnit measurementUnit,
        BigDecimal unitPrice,
        BigDecimal availableQuantity,
        boolean returnable
) implements Serializable {
}
