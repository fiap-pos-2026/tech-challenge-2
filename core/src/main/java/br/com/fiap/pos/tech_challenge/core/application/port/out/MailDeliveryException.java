package br.com.fiap.pos.tech_challenge.core.application.port.out;

/**
 * Raised by a {@link MailPort} implementation when an e-mail cannot be delivered, so the
 * application layer can react without importing any mail framework type.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message) {
        super(message);
    }

    public MailDeliveryException(Throwable cause) {
        super(cause);
    }
}
