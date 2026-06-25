package br.com.fiap.pos.tech_challenge.core.exception;

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
}
