package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.util.Translator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Log4j2
@RequiredArgsConstructor
public class ExceptionHandling {

    private final Translator translator;

    @ExceptionHandler(CoreException.class)
    ResponseEntity<ErrorResponse> handleCoreException(CoreException ex) {
        String message = translator.translate(ex.getMessageKey());
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(message, ex.getErrorCode()));
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<ErrorResponse> handleAccountLockedException(AccountLockedException ex) {
        String message = translator.translate(ex.getMessageKey());
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(message, ex.getErrorCode()));
    }

    @ExceptionHandler(ReturnNotAllowedException.class)
    ResponseEntity<ErrorResponse> handleReturnNotAllowedException(ReturnNotAllowedException ex) {
        String message = translator.translate(ex.getMessageKey());
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(message, ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException() {
        final var error = EApplicationError.MISSING_FIELDS;
        String message = translator.translate(error.getMessageKey());
        return ResponseEntity.status(error.getStatus()).body(new ErrorResponse(message, error.getErrorCode()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage());
        final var error = EApplicationError.INVALID_FIELDS;
        String message = translator.translate(error.getMessageKey());
        return ResponseEntity.status(error.getStatus()).body(new ErrorResponse(message, error.getErrorCode()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        log.warn("Conflito de versão otimista: {}", ex.getMessage());
        final var error = EApplicationError.OPTIMISTIC_LOCK_CONFLICT;
        String message = translator.translate(error.getMessageKey());
        return ResponseEntity.status(error.getStatus()).body(new ErrorResponse(message, error.getErrorCode()));
    }
}
