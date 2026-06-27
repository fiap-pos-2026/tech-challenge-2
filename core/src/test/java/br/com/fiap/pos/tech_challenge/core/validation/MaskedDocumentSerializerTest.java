package br.com.fiap.pos.tech_challenge.core.validation;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
class MaskedDocumentSerializerTest {

    private MaskedDocumentSerializer sut;
    private JsonGenerator gen;

    @BeforeEach
    void setUp() {
        sut = new MaskedDocumentSerializer();
        gen = mock(JsonGenerator.class);
    }

    @Test
    void serialize_writesNullWhenValueIsNull() throws Exception {
        sut.serialize(null, gen, null);

        verify(gen).writeNull();
        verify(gen, never()).writeString(any(String.class));
    }

    @Test
    void serialize_masksCpfCorrectly() throws Exception {
        sut.serialize("529.982.247-25", gen, null);

        verify(gen).writeString("***.982.247-**");
    }

    @Test
    void serialize_masksCpfWithoutMaskCorrectly() throws Exception {
        sut.serialize("52998224725", gen, null);

        verify(gen).writeString("***.982.247-**");
    }

    @Test
    void serialize_masksCnpjCorrectly() throws Exception {
        sut.serialize("11.222.333/0001-81", gen, null);

        verify(gen).writeString("**.222.333/0001-**");
    }

    @Test
    void serialize_passesUnchangedForUnknownFormat() throws Exception {
        sut.serialize("12345", gen, null);

        verify(gen).writeString("12345");
    }
}
