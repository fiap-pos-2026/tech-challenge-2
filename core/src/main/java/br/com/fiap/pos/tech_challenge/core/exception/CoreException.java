package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CoreException extends RuntimeException {

    private final String messageKey;

    private final Integer errorCode;

    private final HttpStatus status;

    public CoreException(EApplicationError error) {
        super(error.getMessageKey());
        this.messageKey = error.getMessageKey();
        this.errorCode = error.getErrorCode();
        this.status = error.getStatus();
    }
}
