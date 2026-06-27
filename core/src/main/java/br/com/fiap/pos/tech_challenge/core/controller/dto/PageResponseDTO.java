package br.com.fiap.pos.tech_challenge.core.controller.dto;

import java.util.List;

/**
 * @author johncgo
 * @since 2026-06-26
 */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
}
