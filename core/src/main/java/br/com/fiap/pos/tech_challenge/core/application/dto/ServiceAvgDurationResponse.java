package br.com.fiap.pos.tech_challenge.core.application.dto;

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
public record ServiceAvgDurationResponse(
        UUID mechanicalServiceUuid,
        String serviceName,
        Double avgEstimatedMinutes,
        Long executionCount
) {}
