package br.com.fiap.pos.tech_challenge.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author johncgo
 * @since 2026-06-24
 */
public class LicensePlateValidator implements ConstraintValidator<ValidLicensePlate, String> {

    // formato antigo: AAA9999 | Mercosul: AAA9A99
    private static final java.util.regex.Pattern PATTERN =
            java.util.regex.Pattern.compile("^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return false;
        return PATTERN.matcher(value.toUpperCase()).matches();
    }
}
