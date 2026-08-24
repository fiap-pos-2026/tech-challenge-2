package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.service.event.ServiceOrderStatusChangedEvent;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author pauloogsouza
 * @since 2026-08-23
 */
@Service
@Log4j2
public class StatusEmailNotifier {

    @Setter(onMethod_ = @Autowired(required = false))
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@tech.com}")
    private String fromAddress;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ServiceOrderStatusChangedEvent event) {
        if (mailSender == null) {
            log.warn("JavaMailSender não configurado — status {} da OS {} não notificado ao cliente",
                    event.newStatus(), event.serviceOrderUuid());
            return;
        }
        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
            log.warn("Cliente sem e-mail — status {} da OS {} não notificado",
                    event.newStatus(), event.serviceOrderUuid());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(event.customerEmail());
            message.setSubject("Atualização da ordem de serviço " + event.serviceOrderUuid());
            message.setText("Olá, " + event.customerName()
                    + "!\nA ordem de serviço " + event.serviceOrderUuid()
                    + " está agora no status " + event.newStatus().name() + ".");
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de status da OS {}: {}",
                    event.serviceOrderUuid(), e.getMessage());
        }
    }
}
