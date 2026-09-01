package br.com.fiap.pos.tech_challenge.core.application.mapper;

import br.com.fiap.pos.tech_challenge.core.application.dto.CreateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.UpdateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.VehicleResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface VehicleMapper {

    Vehicle toEntity(CreateVehicleRequest request);

    @Mapping(target = "customerUuid", source = "customer.uuid")
    VehicleResponse toResponse(Vehicle entity);

    void fullUpdate(UpdateVehicleRequest request, @MappingTarget Vehicle entity);
}
