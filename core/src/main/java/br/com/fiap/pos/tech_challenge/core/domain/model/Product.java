package br.com.fiap.pos.tech_challenge.core.domain.model;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;

    private UUID uuid;

    private String name;

    private String description;

    private ProductType type;

    private MeasurementUnit measurementUnit;

    private BigDecimal unitPrice;

    private BigDecimal availableQuantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean returnable = false;

    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
