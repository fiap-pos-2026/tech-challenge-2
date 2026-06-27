package br.com.fiap.pos.tech_challenge.core.controller.dto;

import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;

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
