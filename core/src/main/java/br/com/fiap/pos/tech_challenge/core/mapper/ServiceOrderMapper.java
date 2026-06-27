package br.com.fiap.pos.tech_challenge.core.mapper;

import br.com.fiap.pos.tech_challenge.core.controller.dto.QuoteProductLineResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.QuoteResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.QuoteServiceLineResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.domain.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.QuoteServiceLine;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ServiceOrderMapper {

    @Mapping(target = "quote", ignore = true)
    ServiceOrderResponse toResponse(ServiceOrder entity);

    QuoteResponse toQuoteResponse(Quote quote);

    @Mapping(target = "mechanicalServiceUuid", source = "mechanicalService.uuid")
    QuoteServiceLineResponse toServiceLineResponse(QuoteServiceLine line);

    @Mapping(target = "productUuid", source = "product.uuid")
    QuoteProductLineResponse toProductLineResponse(QuoteProductLine line);
}
