package br.com.fiap.pos.tech_challenge.core.web.mapper;

import br.com.fiap.pos.tech_challenge.core.application.port.out.PasswordHasher;
import br.com.fiap.pos.tech_challenge.core.web.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.web.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {

    @Autowired
    protected PasswordHasher passwordHasher;

    @Mapping(target = "password", qualifiedByName = "passwordEncoder")
    public abstract User toEntity(CreateUserDTO dto);

    public abstract UserDTO toDTO(User entity);

    public abstract List<UserDTO> toDTOs(List<User> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract User partialUpdate(UpdateUserDTO dto, @MappingTarget User entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", qualifiedByName = "passwordEncoder")
    @Mapping(target = "role", conditionExpression = "java(dto.role() != null)")
    public abstract User fullUpdate(UpdateUserDTO dto, @MappingTarget User entity);

    @Named("passwordEncoder")
    protected String encode(String password) {
        if (StringUtils.isBlank(password)) {
            return null;
        }
        return passwordHasher.hash(password);
    }
}
