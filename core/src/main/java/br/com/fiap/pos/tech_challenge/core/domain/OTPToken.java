package br.com.fiap.pos.tech_challenge.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

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
@Entity
@Table(name = "otp_tokens")
public class OTPToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "_uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_service_order_id", nullable = false)
    private ServiceOrder serviceOrder;

    // SHA-256 hex digest (64 chars)
    @Column(name = "_token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "_expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "_used", nullable = false)
    private boolean used = false;

    @Column(name = "_invalid_attempts", nullable = false)
    private int invalidAttempts = 0;

    @Column(name = "_invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OTPToken that = (OTPToken) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
