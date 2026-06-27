package br.com.fiap.pos.tech_challenge.core.mapper;

import br.com.fiap.pos.tech_challenge.core.controller.dto.StockMovementResponse;
import br.com.fiap.pos.tech_challenge.core.domain.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StockMovementMapper {

    @Mapping(target = "serviceOrderUuid", source = "serviceOrder.uuid")
    StockMovementResponse toResponse(StockMovement movement);
}
