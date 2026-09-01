package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.domain.model.OTPToken;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidOTPTokenException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailDeliveryException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.OTPTokenRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class OTPService {

    private static final int MAX_INVALID_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_HOURS = 1;

    private final OTPTokenRepository otpTokenRepository;

    private final ServiceOrderRepository serviceOrderRepository;

    private final NotificationService notificationService;

    private final MailPort mailPort;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void generateAndSend(ServiceOrder serviceOrder) {
        invalidateByServiceOrder(serviceOrder);

        byte[] rawBytes = new byte[32];
        random.nextBytes(rawBytes);
        String rawToken = HexFormat.of().formatHex(rawBytes);
        String tokenHash = sha256Hex(rawToken);

        OTPToken token = new OTPToken();
        token.setServiceOrder(serviceOrder);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plus(OTP_EXPIRY_HOURS, ChronoUnit.HOURS));
        otpTokenRepository.save(token);

        sendEmail(serviceOrder, rawToken);
    }

    @Transactional
    public void validate(UUID osUuid, String customerDocument, String tokenRaw) {
        Optional<ServiceOrder> osOpt = serviceOrderRepository.findByUuid(osUuid);
        if (osOpt.isEmpty()) {
            throw new InvalidOTPTokenException();
        }
        ServiceOrder serviceOrder = osOpt.get();

        Optional<OTPToken> tokenOpt = otpTokenRepository
                .findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                        serviceOrder.getId(), Instant.now());
        if (tokenOpt.isEmpty()) {
            throw new InvalidOTPTokenException();
        }
        OTPToken token = tokenOpt.get();

        boolean documentMatch = serviceOrder.getCustomer().getDocument().equals(customerDocument);
        boolean tokenMatch = sha256Hex(tokenRaw).equals(token.getTokenHash());

        if (!documentMatch || !tokenMatch) {
            token.setInvalidAttempts(token.getInvalidAttempts() + 1);
            if (token.getInvalidAttempts() > MAX_INVALID_ATTEMPTS) {
                token.setInvalidatedAt(Instant.now());
                notificationService.publishToRole(
                        UserRole.ATTENDANT,
                        NotificationType.OTP_LIMIT_EXCEEDED,
                        "Limite de tentativas OTP atingido para OS " + osUuid,
                        serviceOrder);
            }
            otpTokenRepository.save(token);
            throw new InvalidOTPTokenException();
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    @Transactional
    public void invalidateByServiceOrder(ServiceOrder serviceOrder) {
        otpTokenRepository
                .findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                        serviceOrder.getId(), Instant.now())
                .ifPresent(token -> {
                    token.setInvalidatedAt(Instant.now());
                    otpTokenRepository.save(token);
                });
    }

    private void sendEmail(ServiceOrder serviceOrder, String rawToken) {
        boolean isDelivery = serviceOrder.getStatus() == ServiceOrderStatus.COMPLETED;
        String subject = isDelivery
                ? "Token de confirmação de entrega"
                : "Token de aprovação de orçamento";
        String body = (isDelivery
                ? "Seu token para confirmar ou recusar a entrega do veículo é: "
                : "Seu token de confirmação é: ")
                + rawToken + "\nEste token expira em " + OTP_EXPIRY_HOURS + " hora(s).";
        try {
            mailPort.send(serviceOrder.getCustomer().getEmail(), subject, body);
        } catch (MailDeliveryException e) {
            log.error("Falha ao enviar e-mail OTP para OS {}: {}", serviceOrder.getUuid(), e.getMessage());
            notificationService.publishToRole(
                    UserRole.ATTENDANT,
                    NotificationType.OTP_DELIVERY_FAILED,
                    "Falha ao enviar token OTP para OS " + serviceOrder.getUuid(),
                    serviceOrder);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
