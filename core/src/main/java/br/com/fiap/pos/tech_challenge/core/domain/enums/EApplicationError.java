package br.com.fiap.pos.tech_challenge.core.domain.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ErrorStatus;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum EApplicationError {

    INVALID_USERNAME_PASSWORD("error.invalid_username_password", 1, ErrorStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("error.email_already_exists", 2, ErrorStatus.CONFLICT),
    LOGIN_ALREADY_EXISTS("error.login_already_exists", 3, ErrorStatus.CONFLICT),
    INVALID_FIELDS("error.invalid_fields", 4, ErrorStatus.BAD_REQUEST),
    MISSING_FIELDS("error.missing_fields", 5, ErrorStatus.BAD_REQUEST),
    TOKEN_NOT_SENT("error.token_not_sent", 6, ErrorStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("error.token_expired", 7, ErrorStatus.UNAUTHORIZED),
    INVALID_TOKEN("error.invalid_token", 8, ErrorStatus.UNAUTHORIZED),
    USER_NOT_FOUND("error.user_not_found", 9, ErrorStatus.NOT_FOUND),
    NO_AUTHENTICATION("error.no_authentication", 10, ErrorStatus.BAD_REQUEST),
    INVALID_PRINCIPAL("error.invalid_principal", 11, ErrorStatus.BAD_REQUEST),
    MECHANICAL_SERVICE_IN_ACTIVE_ORDER("error.mechanical_service_in_active_order", 12, ErrorStatus.CONFLICT),
    MECHANICAL_SERVICE_NOT_FOUND("error.mechanical_service_not_found", 13, ErrorStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("error.customer_not_found", 14, ErrorStatus.NOT_FOUND),
    VEHICLE_NOT_FOUND("error.vehicle_not_found", 15, ErrorStatus.NOT_FOUND),
    SERVICE_ORDER_NOT_FOUND("error.service_order_not_found", 16, ErrorStatus.NOT_FOUND),
    QUOTE_NOT_FOUND("error.quote_not_found", 17, ErrorStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND("error.product_not_found", 18, ErrorStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("error.insufficient_stock", 19, ErrorStatus.UNPROCESSABLE_ENTITY),
    INVALID_STATUS_TRANSITION("error.invalid_status_transition", 20, ErrorStatus.CONFLICT),
    DUPLICATE_DOCUMENT("error.duplicate_document", 21, ErrorStatus.CONFLICT),
    DUPLICATE_LICENSE_PLATE("error.duplicate_license_plate", 22, ErrorStatus.CONFLICT),
    INVALID_OTP_SUBMISSION("error.otp_invalid", 23, ErrorStatus.UNPROCESSABLE_ENTITY),
    OTP_LIMIT_EXCEEDED("error.otp_limit_exceeded", 24, ErrorStatus.TOO_MANY_REQUESTS),
    ACCOUNT_LOCKED("error.account_locked", 25, ErrorStatus.LOCKED),
    ACCOUNT_INACTIVE("error.account_inactive", 26, ErrorStatus.UNAUTHORIZED),
    RETURN_NOT_ALLOWED("error.return_not_allowed", 27, ErrorStatus.UNPROCESSABLE_ENTITY),
    RESOURCE_IN_USE("error.resource_in_use", 28, ErrorStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT("error.optimistic_lock_conflict", 29, ErrorStatus.CONFLICT),
    NOTIFICATION_NOT_FOUND("error.notification_not_found", 30, ErrorStatus.NOT_FOUND),
    VEHICLE_NOT_OWNED_BY_CUSTOMER("error.vehicle_not_owned_by_customer", 31, ErrorStatus.UNPROCESSABLE_ENTITY),
    LAST_ADMIN_DELETION_NOT_ALLOWED("error.last_admin_deletion_not_allowed", 32, ErrorStatus.CONFLICT),
    ADMIN_SELF_ROLE_CHANGE_NOT_ALLOWED("error.admin_self_role_change_not_allowed", 33, ErrorStatus.UNPROCESSABLE_ENTITY),
    PASSWORD_CHANGE_REQUIRED("error.password_change_required", 34, ErrorStatus.FORBIDDEN),
    WRONG_CURRENT_PASSWORD("error.wrong_current_password", 35, ErrorStatus.BAD_REQUEST),
    SAME_PASSWORD("error.same_password", 36, ErrorStatus.UNPROCESSABLE_ENTITY),
    TOKEN_BLACKLISTED("error.token_blacklisted", 37, ErrorStatus.UNAUTHORIZED);

    private static final Map<Integer, EApplicationError> BY_ERROR_CODE = new HashMap<>();

    static {
        for (EApplicationError e : values()) {
            BY_ERROR_CODE.put(e.errorCode, e);
        }
    }

    private final String messageKey;

    private final Integer errorCode;

    private final ErrorStatus status;

    public static EApplicationError getByErrorCode(final Integer code) {
        if (code == null) {
            return null;
        }
        return BY_ERROR_CODE.get(code);
    }
}
