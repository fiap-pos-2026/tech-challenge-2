package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.domain.model.Notification;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.application.port.out.NotificationRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class NotificationService {

    private final NotificationRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public void publish(User user, NotificationType type, String message, ServiceOrder serviceOrderRef) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setServiceOrderRef(serviceOrderRef);
        repository.save(notification);
    }

    @Transactional
    public void publishToRole(UserRole role, NotificationType type, String message, ServiceOrder serviceOrderRef) {
        userRepository.findByRole(role).forEach(user -> publish(user, type, message, serviceOrderRef));
    }

    /**
     * Notifica atendentes em transação própria para garantir persistência mesmo que a
     * transação chamadora seja revertida (ex: estoque insuficiente).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishInsufficientStockNotification(String message, ServiceOrder serviceOrderRef) {
        publishToRole(UserRole.ATTENDANT, NotificationType.INSUFFICIENT_STOCK, message, serviceOrderRef);
    }

    @Transactional(readOnly = true)
    public Page<Notification> listByUser(Long userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public Notification markAsRead(UUID uuid, Long userId) {
        Notification notification = repository.findByUuidAndUserId(uuid, userId)
                .orElseThrow(() -> new CoreException(EApplicationError.NOTIFICATION_NOT_FOUND));
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return repository.save(notification);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldReadNotifications() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        repository.deleteByReadTrueAndReadAtBefore(cutoff);
        log.info("Notificações lidas há mais de 90 dias removidas");
    }
}
