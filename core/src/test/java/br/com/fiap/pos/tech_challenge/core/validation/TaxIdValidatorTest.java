package br.com.fiap.pos.tech_challenge.core.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
class TaxIdValidatorTest {

    private TaxIdValidator sut;
    private ConstraintValidatorContext ctx;

    @BeforeEach
    void setUp() {
        sut = new TaxIdValidator();
        ctx = mock(ConstraintValidatorContext.class);
    }

    @Test
    void isValid_returnsTrueForValidCpf() {
        assertThat(sut.isValid("529.982.247-25", ctx)).isTrue();
    }

    @Test
    void isValid_returnsTrueForValidCpfWithoutMask() {
        assertThat(sut.isValid("52998224725", ctx)).isTrue();
    }

    @Test
    void isValid_returnsFalseForAllSameDigitCpf() {
        assertThat(sut.isValid("111.111.111-11", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForInvalidCpfCheckDigit() {
        assertThat(sut.isValid("529.982.247-00", ctx)).isFalse();
    }

    @Test
    void isValid_returnsTrueForValidCnpj() {
        assertThat(sut.isValid("11.222.333/0001-81", ctx)).isTrue();
    }

    @Test
    void isValid_returnsTrueForValidCnpjWithoutMask() {
        assertThat(sut.isValid("11222333000181", ctx)).isTrue();
    }

    @Test
    void isValid_returnsFalseForAllSameDigitCnpj() {
        assertThat(sut.isValid("00.000.000/0000-00", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForInvalidCnpjCheckDigit() {
        assertThat(sut.isValid("11.222.333/0001-00", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForNull() {
        assertThat(sut.isValid(null, ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForBlank() {
        assertThat(sut.isValid("   ", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForWrongLength() {
        assertThat(sut.isValid("12345", ctx)).isFalse();
    }
}
