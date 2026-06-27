package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.domain.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.Vehicle;
import br.com.fiap.pos.tech_challenge.core.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.repository.CustomerRepository;
import br.com.fiap.pos.tech_challenge.core.repository.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.repository.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.repository.VehicleRepository;
import br.com.fiap.pos.tech_challenge.core.service.ServiceOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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

    @MockitoBean JavaMailSender mailSender;

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

    @Test
    void openServiceOrder_persistsWithStatusReceived() {
        var response = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "Barulho no motor");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ServiceOrderStatus.RECEIVED);
        assertThat(response.customerComplaint()).isEqualTo("Barulho no motor");
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
        serviceOrderService.completeDiagnosis(created.uuid()); // must complete within 5 s (SC-002)
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
