package br.com.fiap.pos.tech_challenge.core.infrastructure.mail;

import br.com.fiap.pos.tech_challenge.core.application.port.out.MailDeliveryException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MailPort;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Driven adapter for {@link MailPort}. Wraps Spring's {@link JavaMailSender} and translates
 * any delivery problem (including an unconfigured sender) into {@link MailDeliveryException},
 * so the application layer never sees a Spring mail type.
 */
@Component
@Log4j2
public class SpringMailAdapter implements MailPort {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@tech.com}")
    private String fromAddress;

    @Override
    public void send(String to, String subject, String body) {
        if (mailSender == null) {
            log.warn("JavaMailSender nao configurado - e-mail para {} nao enviado", to);
            throw new MailDeliveryException("mail sender not configured");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail para {}: {}", to, e.getMessage());
            throw new MailDeliveryException(e);
        }
    }
}
