package br.com.fiap.pos.tech_challenge.core.domain.exception;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public class InvalidStatusTransitionException extends CoreException {

    public InvalidStatusTransitionException() {
        super(EApplicationError.INVALID_STATUS_TRANSITION);
    }
}
