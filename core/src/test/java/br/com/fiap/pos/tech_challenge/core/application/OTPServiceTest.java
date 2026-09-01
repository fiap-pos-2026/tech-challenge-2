package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.OTPToken;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidOTPTokenException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.OTPTokenRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailDeliveryException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class OTPServiceTest {

    @Mock OTPTokenRepository otpTokenRepository;
    @Mock ServiceOrderRepository serviceOrderRepository;
    @Mock NotificationService notificationService;
    @Mock MailPort mailPort;

    @InjectMocks OTPService sut;

    @Test
    void generateAndSend_savesHashedToken() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.empty());

        sut.generateAndSend(so);

        ArgumentCaptor<OTPToken> captor = ArgumentCaptor.forClass(OTPToken.class);
        verify(otpTokenRepository, atLeastOnce()).save(captor.capture());
        OTPToken saved = captor.getAllValues().stream()
                .filter(t -> t.getTokenHash() != null).findFirst().orElseThrow();
        assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void generateAndSend_invalidatesPreviousTokenFirst() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        OTPToken existing = new OTPToken();
        existing.setInvalidatedAt(null);

        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.of(existing));

        sut.generateAndSend(so);

        assertThat(existing.getInvalidatedAt()).isNotNull();
    }

    @Test
    void validate_marksTokenUsedOnSuccess() throws Exception {
        UUID osUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        so.setUuid(osUuid);
        Customer customer = new Customer();
        customer.setDocument("52998224725");
        so.setCustomer(customer);

        String rawToken = "myrawtoken";
        String hash = sha256(rawToken);
        OTPToken token = new OTPToken();
        token.setTokenHash(hash);
        token.setUsed(false);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.of(token));

        sut.validate(osUuid, "52998224725", rawToken);

        assertThat(token.isUsed()).isTrue();
        verify(otpTokenRepository, atLeastOnce()).save(token);
    }

    @Test
    void validate_throwsWhenServiceOrderNotFound() {
        UUID osUuid = UUID.randomUUID();
        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.validate(osUuid, "52998224725", "token"))
                .isInstanceOf(InvalidOTPTokenException.class);
    }

    @Test
    void validate_throwsWhenNoActiveToken() {
        UUID osUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        so.setUuid(osUuid);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.validate(osUuid, "52998224725", "token"))
                .isInstanceOf(InvalidOTPTokenException.class);
    }

    @Test
    void validate_throwsAndIncrementsAttemptsOnBadToken() throws Exception {
        UUID osUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        so.setUuid(osUuid);
        Customer customer = new Customer();
        customer.setDocument("52998224725");
        so.setCustomer(customer);

        OTPToken token = new OTPToken();
        token.setTokenHash(sha256("correcttoken"));
        token.setInvalidAttempts(0);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> sut.validate(osUuid, "52998224725", "wrongtoken"))
                .isInstanceOf(InvalidOTPTokenException.class);

        assertThat(token.getInvalidAttempts()).isEqualTo(1);
    }

    @Test
    void validate_invalidatesTokenAfterMaxAttempts() throws Exception {
        UUID osUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        so.setUuid(osUuid);
        Customer customer = new Customer();
        customer.setDocument("52998224725");
        so.setCustomer(customer);

        OTPToken token = new OTPToken();
        token.setTokenHash(sha256("correcttoken"));
        token.setInvalidAttempts(5); // next attempt = 6th → invalidates

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> sut.validate(osUuid, "52998224725", "wrongtoken"))
                .isInstanceOf(InvalidOTPTokenException.class);

        assertThat(token.getInvalidatedAt()).isNotNull();
        verify(notificationService).publishToRole(any(), any(), any(), eq(so));
    }

    @Test
    void invalidateByServiceOrder_setsInvalidatedAtWhenTokenExists() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        OTPToken token = new OTPToken();

        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.of(token));

        sut.invalidateByServiceOrder(so);

        assertThat(token.getInvalidatedAt()).isNotNull();
        verify(otpTokenRepository).save(token);
    }

    @Test
    void invalidateByServiceOrder_doesNothingWhenNoActiveToken() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.empty());

        sut.invalidateByServiceOrder(so);

        verify(otpTokenRepository, never()).save(any());
    }

    @Test
    void generateAndSend_sendsEmailWithApprovalSubjectForNonCompletedStatus() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        Customer customer = new Customer();
        customer.setEmail("cliente@mail.com");
        so.setCustomer(customer);

        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.empty());

        sut.generateAndSend(so);

        verify(mailPort).send(eq("cliente@mail.com"),
                argThat(subject -> subject != null && subject.contains("aprovação")),
                any());
    }

    @Test
    void generateAndSend_sendsEmailWithDeliverySubjectForCompletedStatus() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.COMPLETED);
        Customer customer = new Customer();
        customer.setEmail("cliente@mail.com");
        so.setCustomer(customer);

        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.empty());

        sut.generateAndSend(so);

        verify(mailPort).send(eq("cliente@mail.com"),
                argThat(subject -> subject != null && subject.contains("entrega")),
                any());
    }

    @Test
    void generateAndSend_notifiesAttendantWhenMailDeliveryFails() {
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        Customer customer = new Customer();
        customer.setEmail("cliente@mail.com");
        so.setCustomer(customer);

        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.empty());
        doThrow(new MailDeliveryException("smtp down")).when(mailPort).send(any(), any(), any());

        sut.generateAndSend(so);

        verify(notificationService).publishToRole(any(), any(), any(), eq(so));
    }

    @Test
    void validate_throwsWhenDocumentDoesNotMatch() throws Exception {
        UUID osUuid = UUID.randomUUID();
        ServiceOrder so = serviceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        so.setUuid(osUuid);
        Customer customer = new Customer();
        customer.setDocument("52998224725");
        so.setCustomer(customer);

        String rawToken = "correcttoken";
        OTPToken token = new OTPToken();
        token.setTokenHash(sha256(rawToken));
        token.setInvalidAttempts(0);

        when(serviceOrderRepository.findByUuid(osUuid)).thenReturn(Optional.of(so));
        when(otpTokenRepository.findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                any(), any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> sut.validate(osUuid, "11111111111", rawToken))
                .isInstanceOf(InvalidOTPTokenException.class);

        assertThat(token.getInvalidAttempts()).isEqualTo(1);
    }

    private ServiceOrder serviceOrder(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setStatus(status);
        Customer customer = new Customer();
        customer.setEmail("cliente@mail.com");
        customer.setDocument("52998224725");
        so.setCustomer(customer);
        return so;
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
