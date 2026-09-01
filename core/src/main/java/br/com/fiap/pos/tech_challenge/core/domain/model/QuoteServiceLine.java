package br.com.fiap.pos.tech_challenge.core.domain.model;

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
public class QuoteServiceLine {

    private Long id;

    private Quote quote;

    private MechanicalService mechanicalService;

    private String nameSnapshot;

    private BigDecimal priceSnapshot;

    private int estimatedDurationMinutes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuoteServiceLine other)) return false;
        return id != null && java.util.Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
