package br.com.fiap.pos.tech_challenge.core.application.port.out;

public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message) {
        super(message);
    }

    public MailDeliveryException(Throwable cause) {
        super(cause);
    }
}
