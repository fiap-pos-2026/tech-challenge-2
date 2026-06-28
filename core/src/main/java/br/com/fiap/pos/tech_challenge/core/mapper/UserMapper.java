package br.com.fiap.pos.tech_challenge.core.mapper;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "password", qualifiedByName = "passwordEncoder")
    User toEntity(CreateUserDTO dto);

    UserDTO toDTO(User entity);

    List<UserDTO> toDTOs(List<User> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    User partialUpdate(UpdateUserDTO dto, @MappingTarget User entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", qualifiedByName = "passwordEncoder")
    @Mapping(target = "role", conditionExpression = "java(dto.role() != null)")
    User fullUpdate(UpdateUserDTO dto, @MappingTarget User entity);

    @Named("passwordEncoder")
    default String encode(String password) {
        if (StringUtils.isBlank(password)) {
            return null;
        }
        return new BCryptPasswordEncoder().encode(password);
    }
}
