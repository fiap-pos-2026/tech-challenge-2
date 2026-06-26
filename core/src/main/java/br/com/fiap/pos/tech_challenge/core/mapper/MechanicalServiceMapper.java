package br.com.fiap.pos.tech_challenge.core.mapper;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateServiceRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.MechanicalServiceResponse;
import br.com.fiap.pos.tech_challenge.core.domain.MechanicalService;
import org.mapstruct.*;

import java.util.List;

/**
 * @author johncgo
 * @since 2026-06-25
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MechanicalServiceMapper {

    MechanicalService toEntity(CreateServiceRequest request);

    MechanicalServiceResponse toResponse(MechanicalService entity);

    List<MechanicalServiceResponse> toResponses(List<MechanicalService> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MechanicalService fullUpdate(CreateServiceRequest request, @MappingTarget MechanicalService entity);
}
