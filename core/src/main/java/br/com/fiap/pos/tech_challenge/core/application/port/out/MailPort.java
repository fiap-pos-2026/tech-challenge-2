package br.com.fiap.pos.tech_challenge.core.application.port.out;

public interface MailPort {
    void send(String to, String subject, String body);
}
