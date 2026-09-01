package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.CustomerService;
import br.com.fiap.pos.tech_challenge.core.application.VehicleService;
import br.com.fiap.pos.tech_challenge.core.application.dto.OpenProductItemRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderOpeningServiceTest {

    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock DomainEventPublisher eventPublisher;
    @Mock ServiceOrderMapper mapper;
    @Mock CustomerService customerService;
    @Mock VehicleService vehicleService;
    @Mock MechanicalServiceRepository mechanicalServiceRepository;
    @Mock ProductRepository productRepository;

    ServiceOrderOpeningService sut;

    @BeforeEach
    void setUp() {
        ServiceOrderStore store = new ServiceOrderStore(serviceOrderRepository, eventPublisher);
        QuoteWorkbench quotes = new QuoteWorkbench(quoteRepository);
        ServiceOrderResponseFactory responseFactory = new ServiceOrderResponseFactory(mapper, quotes);
        sut = new ServiceOrderOpeningService(customerService, vehicleService,
                mechanicalServiceRepository, productRepository, quotes, store, responseFactory);
    }

    @Test
    void openServiceOrder_createsOrderWithStatusReceived() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        ServiceOrderResponse expected = responseFor(saved);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(expected);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.openServiceOrder(customerUuid, vehicleUuid, "Barulho no motor");

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
    }

    @Test
    void openServiceOrder_withoutItems_returnsOrderUuidAndSkipsQuote() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        saved.setUuid(osUuid);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(new ServiceOrderResponse(
                osUuid, ServiceOrderStatus.RECEIVED, "Barulho no motor", null, LocalDateTime.now()));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ServiceOrderResponse result = sut.openServiceOrder(
                customerUuid, vehicleUuid, "Barulho no motor", List.of(), List.of());

        assertThat(result.uuid()).isEqualTo(osUuid);
        assertThat(result.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void openServiceOrder_withItems_persistsLinesInProvisionalQuote() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        MechanicalService ms = new MechanicalService();
        ms.setName("Troca de óleo");
        ms.setBasePrice(new BigDecimal("150.00"));
        ms.setEstimatedDurationMinutes(30);

        Product product = new Product();
        product.setName("Óleo 5W30");
        product.setUnitPrice(new BigDecimal("45.00"));

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        Quote quote = new Quote();

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(mechanicalServiceRepository.findByUuid(msUuid)).thenReturn(Optional.of(ms));
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenReturn(quote);
        when(mapper.toResponse(saved)).thenReturn(responseFor(saved));
        when(mapper.toQuoteResponse(quote)).thenReturn(null);

        sut.openServiceOrder(customerUuid, vehicleUuid, "Revisão", List.of(msUuid),
                List.of(new OpenProductItemRequest(productUuid, new BigDecimal("2"))));

        assertThat(quote.getServiceLines()).hasSize(1);
        assertThat(quote.getServiceLines().get(0).getNameSnapshot()).isEqualTo("Troca de óleo");
        assertThat(quote.getProductLines()).hasSize(1);
        assertThat(quote.getProductLines().get(0).getNameSnapshot()).isEqualTo("Óleo 5W30");
        assertThat(quote.getProductLines().get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
        verify(quoteRepository).save(quote);
    }

    @Test
    void openServiceOrder_throwsNotFoundAndPersistsNothingWhenMechanicalServiceMissing() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID msUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(mechanicalServiceRepository.findByUuid(msUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.openServiceOrder(customerUuid, vehicleUuid, "Revisão",
                List.of(msUuid), List.of()))
                .isInstanceOf(MechanicalServiceNotFoundException.class);

        verify(serviceOrderRepository, never()).save(any());
    }

    @Test
    void openServiceOrder_throwsNotFoundAndPersistsNothingWhenProductMissing() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID productUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.openServiceOrder(customerUuid, vehicleUuid, "Revisão",
                List.of(), List.of(new OpenProductItemRequest(productUuid, BigDecimal.ONE))))
                .isInstanceOf(ProductNotFoundException.class);

        verify(serviceOrderRepository, never()).save(any());
    }

    @Test
    void openServiceOrder_publishesStatusEventWithNullPreviousStatus() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setEmail("cliente@mail.com");
        customer.setName("Cliente Teste");

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);

        ServiceOrder saved = serviceOrderWithStatus(ServiceOrderStatus.RECEIVED);
        saved.setUuid(osUuid);
        saved.setCustomer(customer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);
        when(serviceOrderRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(responseFor(saved));
        when(quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        sut.openServiceOrder(customerUuid, vehicleUuid, "Barulho no motor");

        ServiceOrderStatusChangedEvent event = publishedStatusEvent();
        assertThat(event.serviceOrderUuid()).isEqualTo(osUuid);
        assertThat(event.previousStatus()).isNull();
        assertThat(event.newStatus()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(event.customerEmail()).isEqualTo("cliente@mail.com");
        assertThat(event.customerName()).isEqualTo("Cliente Teste");
    }

    @Test
    void openServiceOrder_throwsWhenVehicleDoesNotBelongToCustomer() {
        UUID customerUuid = UUID.randomUUID();
        UUID vehicleUuid = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(1L);

        Customer otherCustomer = new Customer();
        otherCustomer.setId(2L);

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(otherCustomer);

        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(vehicleService.findEntityByUuid(vehicleUuid)).thenReturn(vehicle);

        assertThatThrownBy(() -> sut.openServiceOrder(customerUuid, vehicleUuid, "Queixa"))
                .isInstanceOf(CoreException.class);
    }

    private ServiceOrder serviceOrderWithStatus(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setStatus(status);
        return so;
    }

    private ServiceOrderStatusChangedEvent publishedStatusEvent() {
        ArgumentCaptor<ServiceOrderStatusChangedEvent> captor =
                ArgumentCaptor.forClass(ServiceOrderStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    private ServiceOrderResponse responseFor(ServiceOrder so) {
        return new ServiceOrderResponse(UUID.randomUUID(), so.getStatus(),
                "queixa", null, LocalDateTime.now());
    }
}
