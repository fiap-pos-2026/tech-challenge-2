package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ServiceOrderNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderQueryServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;

    ServiceOrderQueryService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new ServiceOrderQueryService(serviceOrderRepository, store, responseFactory);
    }

    @Test
    void getServiceOrder_returnsResponseWithQuote() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        Quote quote = new Quote();
        ServiceOrderResponse base = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(mapper.toResponse(so)).thenReturn(base);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(mapper.toQuoteResponse(quote)).thenReturn(null);

        sut.getServiceOrder(uuid);

        verify(quoteRepository, times(1)).findFirstByServiceOrderIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getServiceOrder_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getServiceOrder(uuid))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    @Test
    void getServiceOrder_returnsResponse() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrderResponse expected = responseFor(so);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));
        when(mapper.toResponse(so)).thenReturn(expected);

        assertThat(sut.getServiceOrder(uuid)).isEqualTo(expected);
    }

    @Test
    void getServiceOrderStatus_returnsStatusResponse() {
        UUID uuid = UUID.randomUUID();
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        so.setUuid(uuid);

        when(serviceOrderRepository.findByUuid(uuid)).thenReturn(Optional.of(so));

        var result = sut.getServiceOrderStatus(uuid);

        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    @Test
    void listServiceOrders_filtersByStatusWhenProvided() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        when(serviceOrderRepository.findWithFilters(
                eq(ServiceOrderStatus.RECEIVED), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(so)));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(ServiceOrderStatus.RECEIVED, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listServiceOrders_returnsAllWhenNoFilter() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        when(serviceOrderRepository.findWithFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(so)));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(null, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listServiceOrders_appliesExplicitCompletedFilterOverridingDefaultExclusion() {
        ServiceOrder so = serviceOrderWithStatus(ServiceOrderStatus.COMPLETED);
        when(serviceOrderRepository.findWithFilters(
                eq(ServiceOrderStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(so)));
        when(mapper.toResponse(so)).thenReturn(responseFor(so));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(ServiceOrderStatus.COMPLETED, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).extracting(ServiceOrderResponse::status)
                .containsExactly(ServiceOrderStatus.COMPLETED);
        verify(serviceOrderRepository).findWithFilters(
                eq(ServiceOrderStatus.COMPLETED), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listServiceOrders_preservesBusinessPriorityOrderFromQuery() {
        ServiceOrder inProgress = serviceOrderWithStatus(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder awaiting = serviceOrderWithStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        ServiceOrder inDiagnosis = serviceOrderWithStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        ServiceOrder received = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrder cancelled = serviceOrderWithStatus(ServiceOrderStatus.CANCELLED);

        when(serviceOrderRepository.findWithFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inProgress, awaiting, inDiagnosis, received, cancelled)));
        when(mapper.toResponse(inProgress)).thenReturn(responseFor(inProgress));
        when(mapper.toResponse(awaiting)).thenReturn(responseFor(awaiting));
        when(mapper.toResponse(inDiagnosis)).thenReturn(responseFor(inDiagnosis));
        when(mapper.toResponse(received)).thenReturn(responseFor(received));
        when(mapper.toResponse(cancelled)).thenReturn(responseFor(cancelled));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        var page = sut.listServiceOrders(null, null, null, null, Pageable.unpaged());

        assertThat(page.getContent()).extracting(ServiceOrderResponse::status)
                .containsExactly(ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.AWAITING_APPROVAL,
                        ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.RECEIVED,
                        ServiceOrderStatus.CANCELLED);
    }

    @Test
    void listServiceOrders_dropsClientSortKeepingPageAndSize() {
        when(serviceOrderRepository.findWithFilters(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        sut.listServiceOrders(null, null, null, null,
                PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "createdAt")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(serviceOrderRepository).findWithFilters(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(15);
    }

    private ServiceOrder serviceOrderWithStatus(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setStatus(status);
        return so;
    }

    private ServiceOrderResponse responseFor(ServiceOrder so) {
        return new ServiceOrderResponse(UUID.randomUUID(), so.getStatus(),
                "queixa", null, java.time.LocalDateTime.now());
    }
}
