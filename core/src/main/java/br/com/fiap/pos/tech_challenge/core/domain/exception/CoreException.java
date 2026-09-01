package br.com.fiap.pos.tech_challenge.core.domain.exception;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final String messageKey;

    private final Integer errorCode;

    private final ErrorStatus status;

    public CoreException(EApplicationError error) {
        super(error.getMessageKey());
        this.messageKey = error.getMessageKey();
        this.errorCode = error.getErrorCode();
        this.status = error.getStatus();
    }
}
