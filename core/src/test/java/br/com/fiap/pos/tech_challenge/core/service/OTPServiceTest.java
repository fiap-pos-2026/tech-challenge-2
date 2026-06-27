package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.domain.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.OTPToken;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.InvalidOTPTokenException;
import br.com.fiap.pos.tech_challenge.core.repository.OTPTokenRepository;
import br.com.fiap.pos.tech_challenge.core.repository.ServiceOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks OTPService sut;

    // ---- generateAndSend ----

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

    // ---- validate — happy path ----

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

    // ---- validate — failures ----

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

    // ---- invalidateByServiceOrder ----

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

    // ---- helpers ----
    private ServiceOrder serviceOrder(ServiceOrderStatus status) {
        ServiceOrder so = new ServiceOrder();
        so.setStatus(status);
        return so;
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
