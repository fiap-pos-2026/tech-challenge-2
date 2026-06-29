package br.com.fiap.pos.tech_challenge.core.domain;

import br.com.fiap.pos.tech_challenge.core.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.enums.ProductType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "_uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(name = "_name", nullable = false)
    private String name;

    @Column(name = "_description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "_type", nullable = false)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "_measurement_unit", nullable = false)
    private MeasurementUnit measurementUnit;

    @Column(name = "_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "_available_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal availableQuantity;

    @Column(name = "_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "_returnable", nullable = false)
    private boolean returnable = false;

    @Version
    @Column(name = "_version")
    private Long version;

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
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
        Product that = (Product) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
