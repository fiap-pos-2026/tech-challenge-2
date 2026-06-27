package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public class AccountInactiveException extends CoreException {

    public AccountInactiveException() {
        super(EApplicationError.ACCOUNT_INACTIVE);
    }
}
