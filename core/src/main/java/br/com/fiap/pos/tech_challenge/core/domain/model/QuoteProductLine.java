package br.com.fiap.pos.tech_challenge.core.domain.model;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class QuoteProductLine {

    private Long id;

    private Quote quote;

    private Product product;

    private String nameSnapshot;

    private BigDecimal unitPriceSnapshot;

    private BigDecimal quantity;

    private MeasurementUnit measurementUnit;

    private boolean unbudgeted = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuoteProductLine other)) return false;
        return id != null && java.util.Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
