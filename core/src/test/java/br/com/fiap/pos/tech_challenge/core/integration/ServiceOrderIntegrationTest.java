package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.application.dto.OpenProductItemRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import br.com.fiap.pos.tech_challenge.core.domain.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ProductType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CustomerNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.VehicleNotFoundException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CustomerRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.VehicleRepository;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.QuoteApprovalService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderDiagnosisService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderExecutionService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderOpeningService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ErrorStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Transactional
class ServiceOrderIntegrationTest extends BaseIntegrationTest {

    @Autowired ServiceOrderOpeningService openingService;
    @Autowired ServiceOrderDiagnosisService diagnosisService;
    @Autowired QuoteApprovalService approvalService;
    @Autowired ServiceOrderExecutionService executionService;
    @Autowired ServiceOrderQueryService queryService;
    @Autowired ServiceOrderRepository serviceOrderRepository;
    @Autowired QuoteRepository quoteRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired MechanicalServiceRepository mechanicalServiceRepository;
    @Autowired ProductRepository productRepository;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean OTPService otpService;

    private Customer customer;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        serviceOrderRepository.deleteAll();
        vehicleRepository.deleteAll();
        customerRepository.deleteAll();

        customer = new Customer();
        customer.setDocumentType(DocumentType.CPF);
        customer.setDocument("52998224725"); // CPF válido para testes
        customer.setName("Test Customer");
        customer.setEmail("test@tech.com");
        customer = customerRepository.save(customer);

        vehicle = new Vehicle();
        vehicle.setLicensePlate("ABC1D23");
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYear(LocalDate.now().getYear());
        vehicle.setCustomer(customer);
        vehicle = vehicleRepository.save(vehicle);
    }

    private MechanicalService newMechanicalService() {
        MechanicalService service = new MechanicalService();
        service.setName("Troca de óleo");
        service.setBasePrice(new BigDecimal("150.00"));
        service.setEstimatedDurationMinutes(30);
        return service;
    }

    private Product newProduct() {
        Product product = new Product();
        product.setName("Óleo 5W30");
        product.setType(ProductType.PART);
        product.setMeasurementUnit(MeasurementUnit.UNIT);
        product.setUnitPrice(new BigDecimal("45.00"));
        product.setAvailableQuantity(new BigDecimal("10.0000"));
        return product;
    }

    private ServiceOrder persistOrder(ServiceOrderStatus status, LocalDateTime createdAt) {
        ServiceOrder so = new ServiceOrder();
        so.setCustomer(customer);
        so.setVehicle(vehicle);
        so.setCustomerComplaint("Listagem " + status);
        so.setStatus(status);
        so.setCreatedAt(createdAt);
        return serviceOrderRepository.save(so);
    }

    @Test
    void openServiceOrder_persistsWithStatusReceived() {
        var response = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Barulho no motor");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(response.customerComplaint()).isEqualTo("Barulho no motor");
    }

    @Test
    void openServiceOrder_withOptionalItems_persistsProvisionalQuoteLines() {
        MechanicalService service = mechanicalServiceRepository.save(newMechanicalService());
        Product product = productRepository.save(newProduct());

        var response = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Revisão completa",
                List.of(service.getUuid()),
                List.of(new OpenProductItemRequest(product.getUuid(), new BigDecimal("2"))));

        assertThat(response.status()).isEqualTo(ServiceOrderStatus.RECEIVED);

        ServiceOrder persisted = serviceOrderRepository.findByUuid(response.uuid()).orElseThrow();
        var quote = quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(persisted.getId()).orElseThrow();
        assertThat(quote.getServiceLines()).hasSize(1);
        assertThat(quote.getProductLines()).hasSize(1);
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    @Test
    void openServiceOrder_withUnknownProduct_returnsNotFoundAndLeavesNoOrphanOrder() {
        assertThatThrownBy(() -> openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Revisão completa", null,
                List.of(new OpenProductItemRequest(UUID.randomUUID(), BigDecimal.ONE))))
                .isInstanceOf(ProductNotFoundException.class)
                .extracting(e -> ((ProductNotFoundException) e).getStatus())
                .isEqualTo(ErrorStatus.NOT_FOUND);

        assertThat(serviceOrderRepository.count()).isZero();
    }

    @Test
    void openServiceOrder_withUnknownCustomer_returnsNotFoundAndLeavesNoOrphanOrder() {
        assertThatThrownBy(() -> openingService.openServiceOrder(
                UUID.randomUUID(), vehicle.getUuid(), "Barulho no motor"))
                .isInstanceOf(CustomerNotFoundException.class)
                .extracting(e -> ((CustomerNotFoundException) e).getStatus())
                .isEqualTo(ErrorStatus.NOT_FOUND);

        assertThat(serviceOrderRepository.count()).isZero();
    }

    @Test
    void openServiceOrder_withUnknownVehicle_returnsNotFoundAndLeavesNoOrphanOrder() {
        assertThatThrownBy(() -> openingService.openServiceOrder(
                customer.getUuid(), UUID.randomUUID(), "Barulho no motor"))
                .isInstanceOf(VehicleNotFoundException.class)
                .extracting(e -> ((VehicleNotFoundException) e).getStatus())
                .isEqualTo(ErrorStatus.NOT_FOUND);

        assertThat(serviceOrderRepository.count()).isZero();
    }

    @Test
    void getServiceOrderStatus_returnsPersistedStatusForPublicQuery() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Consulta pública");

        var status = queryService.getServiceOrderStatus(created.uuid());

        assertThat(status.uuid()).isEqualTo(created.uuid());
        assertThat(status.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
    }

    @Test
    void approveQuote_transitionsAwaitingApprovalToInProgress() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Aprovação de orçamento");
        diagnosisService.startDiagnosis(created.uuid());
        diagnosisService.completeDiagnosis(created.uuid());

        var approved = approvalService.approveQuote(created.uuid(), customer.getDocument(), "123456");

        assertThat(approved.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    @Test
    void startDiagnosis_transitionsStatusInDatabase() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Freio falhando");

        var inDiagnosis = diagnosisService.startDiagnosis(created.uuid());

        assertThat(inDiagnosis.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
    }

    @Test
    void completeDiagnosis_advancesToAwaitingApproval() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Troca de óleo");

        diagnosisService.startDiagnosis(created.uuid());
        diagnosisService.completeDiagnosis(created.uuid());

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void completeDiagnosis_setsCalculatedTotalAmount() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Verificação geral");

        diagnosisService.startDiagnosis(created.uuid());
        diagnosisService.completeDiagnosis(created.uuid());

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        var quote = quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(persisted.getId());
        assertThat(quote).isPresent();
        assertThat(quote.get().getTotalAmount()).isNotNull();
    }

    @Test
    @Timeout(5)
    void completeDiagnosis_finishesInUnder5Seconds() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Diagnóstico performance");

        diagnosisService.startDiagnosis(created.uuid());
        diagnosisService.completeDiagnosis(created.uuid());
    }

    @Test
    void completeExecution_transitionsInProgressToCompleted() {
        var created = openingService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Reparação motor");

        diagnosisService.startDiagnosis(created.uuid());
        diagnosisService.completeDiagnosis(created.uuid()); // → AWAITING_APPROVAL

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        persisted.setStatus(ServiceOrderStatus.IN_PROGRESS);
        serviceOrderRepository.save(persisted);

        var result = executionService.completeExecution(created.uuid());

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.COMPLETED);
        ServiceOrder finalSo = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(finalSo.getStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
    }

    @Test
    void listServiceOrders_ordersByStatusPriorityAndExcludesCompletedAndDelivered() {
        LocalDateTime base = LocalDateTime.now().minusDays(10);
        ServiceOrder receivedOlder = persistOrder(ServiceOrderStatus.RECEIVED, base);
        ServiceOrder receivedNewer = persistOrder(ServiceOrderStatus.RECEIVED, base.plusDays(1));
        ServiceOrder cancelled = persistOrder(ServiceOrderStatus.CANCELLED, base.plusDays(2));
        ServiceOrder inDiagnosis = persistOrder(ServiceOrderStatus.IN_DIAGNOSIS, base.plusDays(3));
        ServiceOrder awaitingApproval = persistOrder(ServiceOrderStatus.AWAITING_APPROVAL, base.plusDays(4));
        ServiceOrder inProgress = persistOrder(ServiceOrderStatus.IN_PROGRESS, base.plusDays(5));
        ServiceOrder completed = persistOrder(ServiceOrderStatus.COMPLETED, base.plusDays(6));
        ServiceOrder delivered = persistOrder(ServiceOrderStatus.DELIVERED, base.plusDays(7));

        var page = queryService.listServiceOrders(
                null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(ServiceOrderResponse::uuid)
                .containsExactly(inProgress.getUuid(), awaitingApproval.getUuid(), inDiagnosis.getUuid(),
                        receivedOlder.getUuid(), receivedNewer.getUuid(), cancelled.getUuid())
                .doesNotContain(completed.getUuid(), delivered.getUuid());
        assertThat(page.getTotalElements()).isEqualTo(6);
    }

    @Test
    void listServiceOrders_keepsBusinessPriorityWhenClientRequestsDescendingSort() {
        LocalDateTime base = LocalDateTime.now().minusDays(10);
        ServiceOrder receivedOlder = persistOrder(ServiceOrderStatus.RECEIVED, base);
        ServiceOrder receivedNewer = persistOrder(ServiceOrderStatus.RECEIVED, base.plusDays(1));
        ServiceOrder inProgress = persistOrder(ServiceOrderStatus.IN_PROGRESS, base.plusDays(2));

        var page = queryService.listServiceOrders(null, null, null, null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(ServiceOrderResponse::uuid)
                .containsExactly(inProgress.getUuid(), receivedOlder.getUuid(), receivedNewer.getUuid());
    }

    @Test
    void listServiceOrders_withExplicitCompletedFilter_returnsOnlyCompletedOrders() {
        LocalDateTime base = LocalDateTime.now().minusDays(10);
        ServiceOrder completedOlder = persistOrder(ServiceOrderStatus.COMPLETED, base);
        ServiceOrder completedNewer = persistOrder(ServiceOrderStatus.COMPLETED, base.plusDays(1));
        persistOrder(ServiceOrderStatus.DELIVERED, base.plusDays(2));
        persistOrder(ServiceOrderStatus.IN_PROGRESS, base.plusDays(3));

        var page = queryService.listServiceOrders(
                ServiceOrderStatus.COMPLETED, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(ServiceOrderResponse::uuid)
                .containsExactly(completedOlder.getUuid(), completedNewer.getUuid());
        assertThat(page.getContent()).extracting(ServiceOrderResponse::status)
                .containsOnly(ServiceOrderStatus.COMPLETED);
    }
}
