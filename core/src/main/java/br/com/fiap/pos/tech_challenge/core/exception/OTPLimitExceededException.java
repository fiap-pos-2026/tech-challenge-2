package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public class OTPLimitExceededException extends CoreException {

    public OTPLimitExceededException() {
        super(EApplicationError.OTP_LIMIT_EXCEEDED);
    }
}
