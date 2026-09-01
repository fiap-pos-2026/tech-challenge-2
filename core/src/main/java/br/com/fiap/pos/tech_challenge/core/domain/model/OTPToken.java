package br.com.fiap.pos.tech_challenge.core.domain.model;

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
public class OTPToken {

    private Long id;

    private UUID uuid;

    private ServiceOrder serviceOrder;

    private String tokenHash;

    private Instant expiresAt;

    private boolean used = false;

    private int invalidAttempts = 0;

    private Instant invalidatedAt;

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OTPToken other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
