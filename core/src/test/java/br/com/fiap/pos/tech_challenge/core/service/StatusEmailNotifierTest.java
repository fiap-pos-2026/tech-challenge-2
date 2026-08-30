package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.service.event.ServiceOrderStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-08-23
 */
@ExtendWith(MockitoExtension.class)
class StatusEmailNotifierTest {

    @InjectMocks StatusEmailNotifier sut;

    @Test
    void onStatusChanged_listensOnlyAfterCommitSoEmailIsOutsideTheDomainTransaction() throws Exception {
        TransactionalEventListener listener = StatusEmailNotifier.class
                .getDeclaredMethod("onStatusChanged", ServiceOrderStatusChangedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void onStatusChanged_sendsEmailToCustomerWithOrderUuidAndNewStatus() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ReflectionTestUtils.setField(sut, "mailSender", mailSender);
        UUID osUuid = UUID.randomUUID();

        sut.onStatusChanged(new ServiceOrderStatusChangedEvent(osUuid, ServiceOrderStatus.RECEIVED,
                ServiceOrderStatus.IN_DIAGNOSIS, "cliente@mail.com", "Cliente Teste"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("cliente@mail.com");
        assertThat(sent.getText()).contains(osUuid.toString())
                .contains(ServiceOrderStatus.IN_DIAGNOSIS.name());
    }

    @Test
    void onStatusChanged_keepsSilentWhenMailSenderThrowsMailException() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ReflectionTestUtils.setField(sut, "mailSender", mailSender);
        doThrow(mock(MailException.class)).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> sut.onStatusChanged(new ServiceOrderStatusChangedEvent(
                UUID.randomUUID(), ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.COMPLETED,
                "cliente@mail.com", "Cliente Teste")))
                .doesNotThrowAnyException();
    }

    @Test
    void onStatusChanged_keepsSilentWhenMailSenderIsNotConfigured() {
        assertThatCode(() -> sut.onStatusChanged(new ServiceOrderStatusChangedEvent(
                UUID.randomUUID(), null, ServiceOrderStatus.RECEIVED,
                "cliente@mail.com", "Cliente Teste")))
                .doesNotThrowAnyException();
    }

    @Test
    void onStatusChanged_doesNotSendWhenCustomerEmailIsBlank() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ReflectionTestUtils.setField(sut, "mailSender", mailSender);

        assertThatCode(() -> sut.onStatusChanged(new ServiceOrderStatusChangedEvent(
                UUID.randomUUID(), ServiceOrderStatus.RECEIVED, ServiceOrderStatus.CANCELLED,
                "  ", "Cliente Teste")))
                .doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
