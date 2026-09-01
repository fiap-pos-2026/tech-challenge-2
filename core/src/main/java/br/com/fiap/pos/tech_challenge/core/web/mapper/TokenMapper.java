package br.com.fiap.pos.tech_challenge.core.web.mapper;

import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.oauth2.jwt.Jwt;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TokenMapper {

    TokenDTO toDTO(Jwt jwt);
}
