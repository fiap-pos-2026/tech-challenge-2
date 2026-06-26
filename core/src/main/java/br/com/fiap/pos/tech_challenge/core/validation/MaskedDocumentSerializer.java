package br.com.fiap.pos.tech_challenge.core.validation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author johncgo
 * @since 2026-06-24
 */
public class MaskedDocumentSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 11) {
            // CPF: ***.456.789-**
            gen.writeString(String.format("***.%s.%s-**",
                    digits.substring(3, 6),
                    digits.substring(6, 9)));
        } else if (digits.length() == 14) {
            // CNPJ: **.345.678/0001-**
            gen.writeString(String.format("**.%s.%s/%s-**",
                    digits.substring(2, 5),
                    digits.substring(5, 8),
                    digits.substring(8, 12)));
        } else {
            gen.writeString(value);
        }
    }
}
