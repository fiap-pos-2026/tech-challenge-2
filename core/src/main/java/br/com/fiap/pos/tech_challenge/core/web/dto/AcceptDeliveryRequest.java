package br.com.fiap.pos.tech_challenge.core.web.dto;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public record AcceptDeliveryRequest(
        String token,
        String customerDocument
) {
}
