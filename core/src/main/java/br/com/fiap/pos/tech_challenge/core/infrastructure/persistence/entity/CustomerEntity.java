package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity;

import br.com.fiap.pos.tech_challenge.core.domain.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.validation.ValidTaxId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

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
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "_uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "_document_type", nullable = false)
    private DocumentType documentType;

    @ValidTaxId
    @Column(name = "_document", nullable = false, unique = true)
    private String document;

    @Column(name = "_name", nullable = false)
    private String name;

    @Column(name = "_email", nullable = false)
    private String email;

    @Column(name = "_phone")
    private String phone;

    @Column(name = "_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(name = "_version")
    private Long version;

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
        CustomerEntity that = (CustomerEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}