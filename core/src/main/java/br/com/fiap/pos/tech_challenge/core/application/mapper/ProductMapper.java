package br.com.fiap.pos.tech_challenge.core.application.mapper;

import br.com.fiap.pos.tech_challenge.core.application.dto.CreateProductRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.ProductResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import org.mapstruct.*;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    @Mapping(target = "availableQuantity", source = "initialQuantity")
    Product toEntity(CreateProductRequest request);

    ProductResponse toResponse(Product entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    Product fullUpdate(CreateProductRequest request, @MappingTarget Product entity);
}
