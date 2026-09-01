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

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PersistenceMapper {

    CustomerEntity toEntity(Customer model);
    Customer toDomain(CustomerEntity entity);

    VehicleEntity toEntity(Vehicle model);
    Vehicle toDomain(VehicleEntity entity);

    ProductEntity toEntity(Product model);
    Product toDomain(ProductEntity entity);

    MechanicalServiceEntity toEntity(MechanicalService model);
    MechanicalService toDomain(MechanicalServiceEntity entity);

    UserEntity toEntity(User model);
    User toDomain(UserEntity entity);

    SecurityAuditLogEntity toEntity(SecurityAuditLog model);
    SecurityAuditLog toDomain(SecurityAuditLogEntity entity);

    NotificationEntity toEntity(Notification model);
    Notification toDomain(NotificationEntity entity);

    OTPTokenEntity toEntity(OTPToken model);
    OTPToken toDomain(OTPTokenEntity entity);

    StockMovementEntity toEntity(StockMovement model);
    StockMovement toDomain(StockMovementEntity entity);

    ReworkCycleEntity toEntity(ReworkCycle model);
    ReworkCycle toDomain(ReworkCycleEntity entity);

    ServiceOrderEntity toEntity(ServiceOrder model);
    ServiceOrder toDomain(ServiceOrderEntity entity);
    List<ServiceOrder> toServiceOrderDomain(List<ServiceOrderEntity> entities);
    List<ServiceOrderEntity> toServiceOrderEntity(List<ServiceOrder> models);

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
