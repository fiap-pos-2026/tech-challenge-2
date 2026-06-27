package br.com.fiap.pos.tech_challenge.core.exception;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
public class QuoteNotFoundException extends CoreException {

    public QuoteNotFoundException() {
        super(EApplicationError.QUOTE_NOT_FOUND);
    }
}
