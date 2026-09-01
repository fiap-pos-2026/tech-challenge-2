package br.com.fiap.pos.tech_challenge.core.application.dto;

import br.com.fiap.pos.tech_challenge.core.domain.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.validation.MaskedDocumentSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CustomerResponse(
        UUID uuid,
        DocumentType documentType,
        @JsonSerialize(using = MaskedDocumentSerializer.class) String document,
        String name,
        String email,
        String phone,
        LocalDateTime createdAt
) implements Serializable {
}
