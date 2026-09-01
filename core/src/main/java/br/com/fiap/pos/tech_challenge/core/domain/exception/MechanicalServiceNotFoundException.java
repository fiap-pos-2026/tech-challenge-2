package br.com.fiap.pos.tech_challenge.core.domain.exception;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;

/**
 * @author johncgo
 * @since 2026-06-25
 */
public class MechanicalServiceNotFoundException extends CoreException {

    public MechanicalServiceNotFoundException() {
        super(EApplicationError.MECHANICAL_SERVICE_NOT_FOUND);
    }
}
