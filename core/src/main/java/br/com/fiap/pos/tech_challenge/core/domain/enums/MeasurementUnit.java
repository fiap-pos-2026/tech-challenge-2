package br.com.fiap.pos.tech_challenge.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Getter
@RequiredArgsConstructor
public enum MeasurementUnit {
    UNIT(false),
    LITER(true),
    ML(true),
    KG(true),
    GRAM(true),
    METER(true);

    private final boolean fractional;
}
