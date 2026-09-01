package br.com.fiap.pos.tech_challenge.core.application.event;

import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-08-23
 */
public record ServiceOrderStatusChangedEvent(
        UUID serviceOrderUuid,
        ServiceOrderStatus previousStatus,
        ServiceOrderStatus newStatus,
        String customerEmail,
        String customerName
) {
}
