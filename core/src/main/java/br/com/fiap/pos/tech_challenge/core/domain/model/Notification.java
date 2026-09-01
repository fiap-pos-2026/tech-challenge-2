package br.com.fiap.pos.tech_challenge.core.domain.model;

import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
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
public class Notification {

    private Long id;

    private UUID uuid;

    private User user;

    private NotificationType type;

    private String message;

    private ServiceOrder serviceOrderRef;

    private boolean read = false;

    private Instant readAt;

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
