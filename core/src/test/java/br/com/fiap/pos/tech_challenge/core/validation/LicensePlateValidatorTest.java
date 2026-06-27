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
class LicensePlateValidatorTest {

    private LicensePlateValidator sut;
    private ConstraintValidatorContext ctx;

    @BeforeEach
    void setUp() {
        sut = new LicensePlateValidator();
        ctx = mock(ConstraintValidatorContext.class);
    }

    // ---- formato antigo (AAA9999) ----

    @Test
    void isValid_returnsTrueForOldFormat() {
        assertThat(sut.isValid("ABC1234", ctx)).isTrue();
    }

    @Test
    void isValid_returnsTrueForOldFormatLowercase() {
        assertThat(sut.isValid("abc1234", ctx)).isTrue();
    }

    // ---- formato Mercosul (AAA9A99) ----

    @Test
    void isValid_returnsTrueForMercosulFormat() {
        assertThat(sut.isValid("ABC1D23", ctx)).isTrue();
    }

    @Test
    void isValid_returnsTrueForMercosulFormatLowercase() {
        assertThat(sut.isValid("abc1d23", ctx)).isTrue();
    }

    // ---- inválidos ----

    @Test
    void isValid_returnsFalseForNull() {
        assertThat(sut.isValid(null, ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForBlank() {
        assertThat(sut.isValid("   ", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForTooShort() {
        assertThat(sut.isValid("ABC123", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForAllNumbers() {
        assertThat(sut.isValid("1234567", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForAllLetters() {
        assertThat(sut.isValid("ABCDEFG", ctx)).isFalse();
    }

    @Test
    void isValid_returnsFalseForInvalidMercosulPosition() {
        // 5ª posição deveria ser letra, não número
        assertThat(sut.isValid("ABC1234", ctx)).isTrue(); // antigo OK
        assertThat(sut.isValid("ABCD234", ctx)).isFalse(); // 4 letras no começo — inválido
    }
}
