package br.com.fiap.pos.tech_challenge.core.web.mapper;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.CustomerResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import org.mapstruct.Mapper;
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
public interface CustomerMapper {

    Customer toEntity(CreateCustomerRequest request);

    CustomerResponse toResponse(Customer entity);

    void fullUpdate(UpdateCustomerRequest request, @MappingTarget Customer entity);
}
