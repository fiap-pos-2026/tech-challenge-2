package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public class InvalidOTPTokenException extends CoreException {

    public InvalidOTPTokenException() {
        super(EApplicationError.INVALID_OTP_SUBMISSION);
    }
}
