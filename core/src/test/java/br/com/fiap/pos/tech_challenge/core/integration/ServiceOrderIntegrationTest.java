package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.controller.dto.OpenProductItemRequest;
import br.com.fiap.pos.tech_challenge.core.domain.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.Product;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.Vehicle;
import br.com.fiap.pos.tech_challenge.core.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.enums.ProductType;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.repository.CustomerRepository;
import br.com.fiap.pos.tech_challenge.core.repository.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.repository.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.repository.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.repository.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.repository.VehicleRepository;
import br.com.fiap.pos.tech_challenge.core.service.OTPService;
import br.com.fiap.pos.tech_challenge.core.service.ServiceOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Autowired ServiceOrderService serviceOrderService;
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

    @Test
    void openServiceOrder_persistsWithStatusReceived() {
        var response = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Barulho no motor");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(response.customerComplaint()).isEqualTo("Barulho no motor");
    }

    @Test
    void openServiceOrder_withOptionalItems_persistsProvisionalQuoteLines() {
        MechanicalService service = mechanicalServiceRepository.save(newMechanicalService());
        Product product = productRepository.save(newProduct());

        var response = serviceOrderService.openServiceOrder(
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
        assertThatThrownBy(() -> serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Revisão completa", null,
                List.of(new OpenProductItemRequest(UUID.randomUUID(), BigDecimal.ONE))))
                .isInstanceOf(ProductNotFoundException.class)
                .extracting(e -> ((ProductNotFoundException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(serviceOrderRepository.count()).isZero();
    }

    @Test
    void getServiceOrderStatus_returnsPersistedStatusForPublicQuery() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Consulta pública");

        var status = serviceOrderService.getServiceOrderStatus(created.uuid());

        assertThat(status.uuid()).isEqualTo(created.uuid());
        assertThat(status.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
    }

    @Test
    void approveQuote_transitionsAwaitingApprovalToInProgress() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Aprovação de orçamento");
        serviceOrderService.startDiagnosis(created.uuid());
        serviceOrderService.completeDiagnosis(created.uuid());

        var approved = serviceOrderService.approveQuote(created.uuid(), customer.getDocument(), "123456");

        assertThat(approved.status()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.IN_PROGRESS);
    }

    @Test
    void startDiagnosis_transitionsStatusInDatabase() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Freio falhando");

        var inDiagnosis = serviceOrderService.startDiagnosis(created.uuid());

        assertThat(inDiagnosis.status()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
    }

    @Test
    void completeDiagnosis_advancesToAwaitingApproval() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Troca de óleo");

        serviceOrderService.startDiagnosis(created.uuid());
        serviceOrderService.completeDiagnosis(created.uuid());

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void completeDiagnosis_setsCalculatedTotalAmount() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Verificação geral");

        serviceOrderService.startDiagnosis(created.uuid());
        serviceOrderService.completeDiagnosis(created.uuid());

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        var quote = quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(persisted.getId());
        assertThat(quote).isPresent();
        assertThat(quote.get().getTotalAmount()).isNotNull();
    }

    @Test
    @Timeout(5)
    void completeDiagnosis_finishesInUnder5Seconds() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Diagnóstico performance");

        serviceOrderService.startDiagnosis(created.uuid());
        serviceOrderService.completeDiagnosis(created.uuid());
    }

    @Test
    void completeExecution_transitionsInProgressToCompleted() {
        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Reparação motor");

        serviceOrderService.startDiagnosis(created.uuid());
        serviceOrderService.completeDiagnosis(created.uuid()); // → AWAITING_APPROVAL

        // advance to IN_PROGRESS directly (simulates quote approval)
        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        persisted.setStatus(ServiceOrderStatus.IN_PROGRESS);
        serviceOrderRepository.save(persisted);

        var result = serviceOrderService.completeExecution(created.uuid());

        assertThat(result.status()).isEqualTo(ServiceOrderStatus.COMPLETED);
        ServiceOrder finalSo = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(finalSo.getStatus()).isEqualTo(ServiceOrderStatus.COMPLETED);
    }
}
