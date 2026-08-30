package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.domain.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.Vehicle;
import br.com.fiap.pos.tech_challenge.core.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.repository.CustomerRepository;
import br.com.fiap.pos.tech_challenge.core.repository.ServiceOrderRepository;
import br.com.fiap.pos.tech_challenge.core.repository.VehicleRepository;
import br.com.fiap.pos.tech_challenge.core.service.OTPService;
import br.com.fiap.pos.tech_challenge.core.service.ServiceOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Sem {@code @Transactional}: o e-mail de status é despachado em {@code AFTER_COMMIT}, então só um
 * commit real exercita a garantia de que a falha de SMTP não desfaz a transição.
 *
 * @author pauloogsouza
 * @since 2026-08-24
 */
class StatusEmailNotifierIntegrationTest extends BaseIntegrationTest {

    @Autowired ServiceOrderService serviceOrderService;
    @Autowired ServiceOrderRepository serviceOrderRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired VehicleRepository vehicleRepository;

    @MockitoBean JavaMailSender mailSender;
    @MockitoBean OTPService otpService;

    private Customer customer;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setDocumentType(DocumentType.CPF);
        customer.setDocument("52998224725"); // CPF válido para testes
        customer.setName("Cliente SMTP");
        customer.setEmail("cliente.smtp@tech.com");
        customer = customerRepository.save(customer);

        vehicle = new Vehicle();
        vehicle.setLicensePlate("SMT1P23");
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYear(LocalDate.now().getYear());
        vehicle.setCustomer(customer);
        vehicle = vehicleRepository.save(vehicle);
    }

    @AfterEach
    void tearDown() {
        serviceOrderRepository.deleteAll();
        vehicleRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void statusTransition_keepsCommittedStatusWhenSmtpFails() {
        doThrow(new MailSendException("SMTP indisponível"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        var created = serviceOrderService.openServiceOrder(
                customer.getUuid(), vehicle.getUuid(), "SMTP fora do ar");

        assertThatCode(() -> serviceOrderService.startDiagnosis(created.uuid()))
                .doesNotThrowAnyException();

        ServiceOrder persisted = serviceOrderRepository.findByUuid(created.uuid()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ServiceOrderStatus.IN_DIAGNOSIS);
        verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class));
    }
}
