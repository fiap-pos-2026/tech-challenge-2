package br.com.fiap.pos.tech_challenge.core.domain.exception;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public class VehicleNotFoundException extends CoreException {

    public VehicleNotFoundException() {
        super(EApplicationError.VEHICLE_NOT_FOUND);
    }
}
