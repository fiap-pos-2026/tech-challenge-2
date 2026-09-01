package br.com.fiap.pos.tech_challenge.core.domain.model;

import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
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
public class SecurityAuditLog {

    private Long id;

    private UUID uuid;

    private Instant eventTimestamp;

    private AuditEventType eventType;

    private User user;

    private String attemptIdentifier;

    private String result;

    private String details;

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityAuditLog other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
