package br.com.fiap.pos.tech_challenge.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author johncgo
 * @since 2026-06-24
 */
public class TaxIdValidator implements ConstraintValidator<ValidTaxId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return false;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 11) return isValidCpf(digits);
        if (digits.length() == 14) return isValidCnpj(digits);
        return false;
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (cpf.charAt(i) - '0') * (10 - i);
        int d1 = 11 - (sum % 11);
        if (d1 >= 10) d1 = 0;
        if (d1 != cpf.charAt(9) - '0') return false;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += (cpf.charAt(i) - '0') * (11 - i);
        int d2 = 11 - (sum % 11);
        if (d2 >= 10) d2 = 0;
        return d2 == cpf.charAt(10) - '0';
    }

    private boolean isValidCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (cnpj.charAt(i) - '0') * w1[i];
        int d1 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
        if (d1 != cnpj.charAt(12) - '0') return false;
        sum = 0;
        for (int i = 0; i < 13; i++) sum += (cnpj.charAt(i) - '0') * w2[i];
        int d2 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
        return d2 == cnpj.charAt(13) - '0';
    }
}
