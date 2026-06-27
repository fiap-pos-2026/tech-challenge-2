package br.com.fiap.pos.tech_challenge.core.controller.dto;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record AcceptDeliveryRequest(
        String token,
        String customerDocument
) {
}
