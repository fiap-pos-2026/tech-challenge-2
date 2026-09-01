package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderStatusResponse;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ServiceOrderResponseFactory {

    private final ServiceOrderMapper mapper;
    private final QuoteWorkbench quotes;

    ServiceOrderResponse toResponse(ServiceOrder so) {
        ServiceOrderResponse base = mapper.toResponse(so);
        return quotes.findLatest(so)
                .map(q -> new ServiceOrderResponse(
                        base.uuid(), base.status(), base.customerComplaint(),
                        mapper.toQuoteResponse(q), base.createdAt()))
                .orElse(base);
    }

    ServiceOrderStatusResponse toStatusResponse(ServiceOrder so) {
        return new ServiceOrderStatusResponse(so.getUuid(), so.getStatus(), so.getUpdatedAt());
    }
}
