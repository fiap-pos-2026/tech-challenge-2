package br.com.fiap.pos.tech_challenge.core.enums;

/**
 * @author johncgo
 * @since 2026-06-24
 */
public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCESS_DENIED,
    REAUTHENTICATION_SUCCESS,
    REAUTHENTICATION_FAILED,
    OTP_INVALID_LIMIT,
    DISPUTED_CLOSURE,
    PRODUCT_RETURN
}
