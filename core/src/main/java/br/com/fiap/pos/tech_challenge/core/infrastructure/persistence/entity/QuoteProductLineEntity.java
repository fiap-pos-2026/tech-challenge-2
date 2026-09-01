package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity;

import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quote_product_lines")
public class QuoteProductLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_quote_id", nullable = false)
    private QuoteEntity quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "_name_snapshot", nullable = false)
    private String nameSnapshot;

    @Column(name = "_unit_price_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "_measurement_unit", nullable = false)
    private MeasurementUnit measurementUnit;

    @Column(name = "_unbudgeted", nullable = false)
    private boolean unbudgeted = false;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        QuoteProductLineEntity that = (QuoteProductLineEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}