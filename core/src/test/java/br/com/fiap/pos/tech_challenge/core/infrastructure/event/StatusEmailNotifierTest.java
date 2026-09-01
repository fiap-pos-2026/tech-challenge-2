package br.com.fiap.pos.tech_challenge.core.infrastructure.event;

import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailDeliveryException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailPort;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatusEmailNotifierTest {

    @Mock MailPort mailPort;
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
        UUID osUuid = UUID.randomUUID();

        sut.onStatusChanged(new ServiceOrderStatusChangedEvent(osUuid, ServiceOrderStatus.RECEIVED,
                ServiceOrderStatus.IN_DIAGNOSIS, "cliente@mail.com", "Cliente Teste"));

        verify(mailPort).send(
                eq("cliente@mail.com"),
                contains(osUuid.toString()),
                contains(ServiceOrderStatus.IN_DIAGNOSIS.name()));
    }

    @Test
    void onStatusChanged_keepsSilentWhenDeliveryFails() {
        doThrow(new MailDeliveryException("smtp down")).when(mailPort).send(any(), any(), any());

        assertThatCode(() -> sut.onStatusChanged(new ServiceOrderStatusChangedEvent(
                UUID.randomUUID(), ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.COMPLETED,
                "cliente@mail.com", "Cliente Teste")))
                .doesNotThrowAnyException();
    }

    @Test
    void onStatusChanged_keepsSilentWhenMailSenderIsNotConfigured() {
        doThrow(new MailDeliveryException("mail sender not configured")).when(mailPort).send(any(), any(), any());

        assertThatCode(() -> sut.onStatusChanged(new ServiceOrderStatusChangedEvent(
                UUID.randomUUID(), null, ServiceOrderStatus.RECEIVED,
                "cliente@mail.com", "Cliente Teste")))
                .doesNotThrowAnyException();
    }

    @Test
    void onStatusChanged_doesNotSendWhenCustomerEmailIsBlank() {
        assertThatCode(() -> sut.onStatusChanged(new ServiceOrderStatusChangedEvent(
                UUID.randomUUID(), ServiceOrderStatus.RECEIVED, ServiceOrderStatus.CANCELLED,
                "  ", "Cliente Teste")))
                .doesNotThrowAnyException();

        verify(mailPort, never()).send(any(), any(), any());
    }
}
