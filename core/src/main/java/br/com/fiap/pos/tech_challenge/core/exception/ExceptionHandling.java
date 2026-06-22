package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Log4j2
public class ExceptionHandling {

    @ExceptionHandler(CoreException.class)
    ResponseEntity<ErrorResponse> handleCoreException(CoreException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.toErrorResponse());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException() {
        final var error = EApplicationError.MISSING_FIELDS;
        return ResponseEntity.status(error.getStatus()).body(new ErrorResponse(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage());
        final var error = EApplicationError.INVALID_FIELDS;
        return ResponseEntity.status(error.getStatus()).body(new ErrorResponse(error));
    }
}
