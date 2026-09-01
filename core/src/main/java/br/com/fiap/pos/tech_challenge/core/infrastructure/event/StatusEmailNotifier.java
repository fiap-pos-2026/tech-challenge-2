package br.com.fiap.pos.tech_challenge.core.infrastructure.event;

import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailDeliveryException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Log4j2
public class StatusEmailNotifier {

    private final MailPort mailPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ServiceOrderStatusChangedEvent event) {
        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
            log.warn("Cliente sem e-mail - status {} da OS {} nao notificado",
                    event.newStatus(), event.serviceOrderUuid());
            return;
        }
        try {
            mailPort.send(
                    event.customerEmail(),
                    "Atualizacao da ordem de servico " + event.serviceOrderUuid(),
                    "Ola, " + event.customerName() + "!\nA ordem de servico " + event.serviceOrderUuid()
                            + " esta agora no status " + event.newStatus().name() + ".");
        } catch (MailDeliveryException e) {
            log.error("Falha ao enviar e-mail de status da OS {}: {}",
                    event.serviceOrderUuid(), e.getMessage());
        }
    }
}
