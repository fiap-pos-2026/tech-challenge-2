package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@RequiredArgsConstructor
@Getter
public final class ErrorResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private final String message;

    private final Integer errorCode;

    public ErrorResponse(final EApplicationError error) {
        this.message = error.getMessage();
        this.errorCode = error.getErrorCode();
    }
}
