package br.com.fiap.pos.tech_challenge.core.web.mapper;

import br.com.fiap.pos.tech_challenge.core.web.dto.QuoteProductLineResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.QuoteResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.QuoteServiceLineResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteServiceLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
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
