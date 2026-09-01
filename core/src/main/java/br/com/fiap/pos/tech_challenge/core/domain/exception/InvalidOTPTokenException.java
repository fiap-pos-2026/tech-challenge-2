package br.com.fiap.pos.tech_challenge.core.domain.exception;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public class InvalidOTPTokenException extends CoreException {

    public InvalidOTPTokenException() {
        super(EApplicationError.INVALID_OTP_SUBMISSION);
    }
}
