package br.com.fiap.pos.tech_challenge.core.application.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public record CreateProductRequest(
        @NotBlank String name,
        @Size(max = 500) String description,
        @NotNull ProductType type,
        @NotNull MeasurementUnit measurementUnit,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @NotNull @DecimalMin("0.00") BigDecimal initialQuantity,
        @NotNull Boolean returnable
) {
}
