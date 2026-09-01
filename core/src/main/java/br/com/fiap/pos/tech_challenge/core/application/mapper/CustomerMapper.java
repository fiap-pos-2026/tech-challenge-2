package br.com.fiap.pos.tech_challenge.core.application.mapper;

import br.com.fiap.pos.tech_challenge.core.application.dto.CreateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.CustomerResponse;
import br.com.fiap.pos.tech_challenge.core.application.dto.UpdateCustomerRequest;
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
