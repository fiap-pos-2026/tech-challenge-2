package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderStatusResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderQueryService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional(readOnly = true)
    public ServiceOrderStatusResponse getServiceOrderStatus(UUID osUuid) {
        return responseFactory.toStatusResponse(store.findByUuidOrThrow(osUuid));
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponse getServiceOrder(UUID osUuid) {
        return responseFactory.toResponse(store.findByUuidOrThrow(osUuid));
    }

    @Transactional(readOnly = true)
    public Page<ServiceOrderResponse> listServiceOrders(ServiceOrderStatus status, UUID customerUuid,
                                                        LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return serviceOrderRepository
                .findWithFilters(status, customerUuid, from, to, withoutClientSort(pageable))
                .map(responseFactory::toResponse);
    }

    private Pageable withoutClientSort(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
    }
}
