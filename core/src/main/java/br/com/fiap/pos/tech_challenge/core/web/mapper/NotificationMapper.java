package br.com.fiap.pos.tech_challenge.core.web.mapper;

import br.com.fiap.pos.tech_challenge.core.web.dto.NotificationResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Notification;
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
public interface NotificationMapper {

    @Mapping(target = "serviceOrderRefId", source = "serviceOrderRef.uuid")
    NotificationResponse toResponse(Notification notification);
}
