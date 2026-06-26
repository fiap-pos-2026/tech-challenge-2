package br.com.fiap.pos.tech_challenge.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Documented
@Constraint(validatedBy = LicensePlateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLicensePlate {
    String message() default "Placa inválida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
