package br.com.fiap.pos.tech_challenge.core.domain.model;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MovementType;
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
public class StockMovement {

    private Long id;

    private UUID uuid;

    private Product product;

    private ServiceOrder serviceOrder;

    private MovementType type;

    private BigDecimal quantity;

    private BigDecimal referenceUnitPrice;

    private User user;

    private String notes;

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockMovement other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
