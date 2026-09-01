package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper;

import br.com.fiap.pos.tech_challenge.core.domain.model.*;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Bidirectional mapping between framework-free domain models and JPA persistence entities.
 * One interface so MapStruct resolves every nested aggregate mapping internally. The
 * {@code Quote} aggregate is a parent/child cycle: the child {@code quote} back-reference
 * is ignored during mapping and restored in {@link #link(QuoteEntity)} / {@link #link(Quote)}.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PersistenceMapper {

    // ---- Customer ----
    CustomerEntity toEntity(Customer model);
    Customer toDomain(CustomerEntity entity);

    // ---- Vehicle ----
    VehicleEntity toEntity(Vehicle model);
    Vehicle toDomain(VehicleEntity entity);

    // ---- Product ----
    ProductEntity toEntity(Product model);
    Product toDomain(ProductEntity entity);

    // ---- MechanicalService ----
    MechanicalServiceEntity toEntity(MechanicalService model);
    MechanicalService toDomain(MechanicalServiceEntity entity);

    // ---- User ----
    UserEntity toEntity(User model);
    User toDomain(UserEntity entity);

    // ---- SecurityAuditLog ----
    SecurityAuditLogEntity toEntity(SecurityAuditLog model);
    SecurityAuditLog toDomain(SecurityAuditLogEntity entity);

    // ---- Notification ----
    NotificationEntity toEntity(Notification model);
    Notification toDomain(NotificationEntity entity);

    // ---- OTPToken ----
    OTPTokenEntity toEntity(OTPToken model);
    OTPToken toDomain(OTPTokenEntity entity);

    // ---- StockMovement ----
    StockMovementEntity toEntity(StockMovement model);
    StockMovement toDomain(StockMovementEntity entity);

    // ---- ReworkCycle ----
    ReworkCycleEntity toEntity(ReworkCycle model);
    ReworkCycle toDomain(ReworkCycleEntity entity);

    // ---- ServiceOrder ----
    ServiceOrderEntity toEntity(ServiceOrder model);
    ServiceOrder toDomain(ServiceOrderEntity entity);
    List<ServiceOrder> toServiceOrderDomain(List<ServiceOrderEntity> entities);
    List<ServiceOrderEntity> toServiceOrderEntity(List<ServiceOrder> models);

    // ---- Quote aggregate (parent/child cycle) ----
    @Mapping(target = "quote", ignore = true)
    QuoteProductLineEntity toEntity(QuoteProductLine model);
    @Mapping(target = "quote", ignore = true)
    QuoteProductLine toDomain(QuoteProductLineEntity entity);

    @Mapping(target = "quote", ignore = true)
    QuoteServiceLineEntity toEntity(QuoteServiceLine model);
    @Mapping(target = "quote", ignore = true)
    QuoteServiceLine toDomain(QuoteServiceLineEntity entity);

    QuoteEntity toEntity(Quote model);
    Quote toDomain(QuoteEntity entity);

    @AfterMapping
    default void link(@MappingTarget QuoteEntity quote) {
        if (quote.getProductLines() != null) {
            quote.getProductLines().forEach(l -> l.setQuote(quote));
        }
        if (quote.getServiceLines() != null) {
            quote.getServiceLines().forEach(l -> l.setQuote(quote));
        }
    }

    @AfterMapping
    default void link(@MappingTarget Quote quote) {
        if (quote.getProductLines() != null) {
            quote.getProductLines().forEach(l -> l.setQuote(quote));
        }
        if (quote.getServiceLines() != null) {
            quote.getServiceLines().forEach(l -> l.setQuote(quote));
        }
    }
}
