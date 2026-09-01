package br.com.fiap.pos.tech_challenge.core.application.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
public record ServiceOrderStatusResponse(
        UUID uuid,
        ServiceOrderStatus status,
        LocalDateTime updatedAt
) {
}
