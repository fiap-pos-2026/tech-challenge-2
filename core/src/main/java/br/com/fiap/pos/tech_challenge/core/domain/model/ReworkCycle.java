package br.com.fiap.pos.tech_challenge.core.domain.model;

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
public class ReworkCycle {

    private Long id;

    private UUID uuid;

    private ServiceOrder serviceOrder;

    private String rejectionReason;

    private Integer estimatedDurationMinutes;

    private BigDecimal estimatedInternalCost;

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReworkCycle other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
