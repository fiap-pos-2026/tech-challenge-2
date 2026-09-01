package br.com.fiap.pos.tech_challenge.core.application.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        UUID uuid,
        NotificationType type,
        String message,
        UUID serviceOrderRefId,
        boolean read,
        LocalDateTime createdAt
) implements Serializable {
}
