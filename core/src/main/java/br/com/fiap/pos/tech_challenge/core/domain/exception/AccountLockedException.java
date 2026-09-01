package br.com.fiap.pos.tech_challenge.core.domain.exception;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public class AccountLockedException extends CoreException {

    public AccountLockedException() {
        super(EApplicationError.ACCOUNT_LOCKED);
    }
}
