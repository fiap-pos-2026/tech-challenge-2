package br.com.fiap.pos.tech_challenge.core.enums;

/**
 * @author johncgo
 * @since 2026-06-24
 */
public enum ServiceOrderStatus {
    RECEIVED,
    IN_DIAGNOSIS,
    AWAITING_APPROVAL,
    IN_PROGRESS,
    COMPLETED,
    DELIVERED,
    CANCELLED,
    DISPUTED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == DISPUTED;
    }
}
