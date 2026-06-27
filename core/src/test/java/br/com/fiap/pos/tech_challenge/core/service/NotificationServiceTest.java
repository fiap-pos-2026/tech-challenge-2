package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.domain.Notification;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.repository.NotificationRepository;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repository;
    @Mock UserRepository userRepository;

    @InjectMocks NotificationService sut;

    // ---- publish ----

    @Test
    void publish_savesNotificationWithAllFields() {
        User user = new User();
        ServiceOrder so = new ServiceOrder();
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        sut.publish(user, NotificationType.QUOTE_EXPIRED, "orçamento expirado", so);

        verify(repository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getType()).isEqualTo(NotificationType.QUOTE_EXPIRED);
        assertThat(saved.getMessage()).isEqualTo("orçamento expirado");
        assertThat(saved.getServiceOrderRef()).isSameAs(so);
    }

    // ---- publishToRole ----

    @Test
    void publishToRole_publishesToAllUsersWithRole() {
        User u1 = new User();
        User u2 = new User();
        when(userRepository.findByRole(UserRole.ATTENDANT)).thenReturn(List.of(u1, u2));

        sut.publishToRole(UserRole.ATTENDANT, NotificationType.INSUFFICIENT_STOCK, "msg", new ServiceOrder());

        verify(repository, times(2)).save(any());
    }

    @Test
    void publishToRole_doesNothingWhenNoUsersWithRole() {
        when(userRepository.findByRole(UserRole.MECHANIC)).thenReturn(List.of());

        sut.publishToRole(UserRole.MECHANIC, NotificationType.QUOTE_EXPIRED, "msg", new ServiceOrder());

        verify(repository, never()).save(any());
    }

    // ---- publishInsufficientStockNotification ----

    @Test
    void publishInsufficientStockNotification_publishesToAttendants() {
        User attendant = new User();
        when(userRepository.findByRole(UserRole.ATTENDANT)).thenReturn(List.of(attendant));

        sut.publishInsufficientStockNotification("estoque insuficiente", new ServiceOrder());

        verify(repository).save(any(Notification.class));
    }

    // ---- markAsRead ----

    @Test
    void markAsRead_setsReadAndReadAt() {
        UUID uuid = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setRead(false);

        when(repository.findByUuidAndUserId(uuid, 1L)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        Notification result = sut.markAsRead(uuid, 1L);

        assertThat(result.isRead()).isTrue();
        assertThat(result.getReadAt()).isNotNull();
    }

    @Test
    void markAsRead_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuidAndUserId(uuid, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.markAsRead(uuid, 1L))
                .isInstanceOf(CoreException.class);
    }

    // ---- listByUser ----

    @Test
    void listByUser_delegatesToRepository() {
        when(repository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        sut.listByUser(1L, Pageable.unpaged());

        verify(repository).findByUserIdOrderByCreatedAtDesc(eq(1L), any());
    }

    // ---- cleanupOldReadNotifications ----

    @Test
    void cleanupOldReadNotifications_deletesOldRead() {
        sut.cleanupOldReadNotifications();

        verify(repository).deleteByReadTrueAndReadAtBefore(any());
    }
}
