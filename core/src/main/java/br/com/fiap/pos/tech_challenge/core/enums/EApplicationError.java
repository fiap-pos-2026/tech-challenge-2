package br.com.fiap.pos.tech_challenge.core.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum EApplicationError {

    INVALID_USERNAME_PASSWORD("error.invalid_username_password", 1, HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("error.email_already_exists", 2, HttpStatus.CONFLICT),
    LOGIN_ALREADY_EXISTS("error.login_already_exists", 3, HttpStatus.CONFLICT),
    INVALID_FIELDS("error.invalid_fields", 4, HttpStatus.BAD_REQUEST),
    MISSING_FIELDS("error.missing_fields", 5, HttpStatus.BAD_REQUEST),
    TOKEN_NOT_SENT("error.token_not_sent", 6, HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("error.token_expired", 7, HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("error.invalid_token", 8, HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("error.user_not_found", 9, HttpStatus.NOT_FOUND),
    NO_AUTHENTICATION("error.no_authentication", 10, HttpStatus.BAD_REQUEST),
    INVALID_PRINCIPAL("error.invalid_principal", 11, HttpStatus.BAD_REQUEST),
    MECHANICAL_SERVICE_IN_ACTIVE_ORDER("error.mechanical_service_in_active_order", 12, HttpStatus.CONFLICT),
    MECHANICAL_SERVICE_NOT_FOUND("error.mechanical_service_not_found", 13, HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("error.customer_not_found", 14, HttpStatus.NOT_FOUND),
    VEHICLE_NOT_FOUND("error.vehicle_not_found", 15, HttpStatus.NOT_FOUND),
    SERVICE_ORDER_NOT_FOUND("error.service_order_not_found", 16, HttpStatus.NOT_FOUND),
    QUOTE_NOT_FOUND("error.quote_not_found", 17, HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND("error.product_not_found", 18, HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("error.insufficient_stock", 19, HttpStatus.UNPROCESSABLE_CONTENT),
    INVALID_STATUS_TRANSITION("error.invalid_status_transition", 20, HttpStatus.CONFLICT),
    DUPLICATE_DOCUMENT("error.duplicate_document", 21, HttpStatus.CONFLICT),
    DUPLICATE_LICENSE_PLATE("error.duplicate_license_plate", 22, HttpStatus.CONFLICT),
    INVALID_OTP_SUBMISSION("error.otp_invalid", 23, HttpStatus.UNPROCESSABLE_CONTENT),
    OTP_LIMIT_EXCEEDED("error.otp_limit_exceeded", 24, HttpStatus.TOO_MANY_REQUESTS),
    ACCOUNT_LOCKED("error.account_locked", 25, HttpStatus.LOCKED),
    ACCOUNT_INACTIVE("error.account_inactive", 26, HttpStatus.UNAUTHORIZED),
    RETURN_NOT_ALLOWED("error.return_not_allowed", 27, HttpStatus.UNPROCESSABLE_CONTENT),
    RESOURCE_IN_USE("error.resource_in_use", 28, HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT("error.optimistic_lock_conflict", 29, HttpStatus.CONFLICT),
    NOTIFICATION_NOT_FOUND("error.notification_not_found", 30, HttpStatus.NOT_FOUND),
    VEHICLE_NOT_OWNED_BY_CUSTOMER("error.vehicle_not_owned_by_customer", 31, HttpStatus.UNPROCESSABLE_CONTENT),
    LAST_ADMIN_DELETION_NOT_ALLOWED("error.last_admin_deletion_not_allowed", 32, HttpStatus.CONFLICT),
    ADMIN_SELF_ROLE_CHANGE_NOT_ALLOWED("error.admin_self_role_change_not_allowed", 33, HttpStatus.UNPROCESSABLE_CONTENT),
    PASSWORD_CHANGE_REQUIRED("error.password_change_required", 34, HttpStatus.FORBIDDEN),
    WRONG_CURRENT_PASSWORD("error.wrong_current_password", 35, HttpStatus.BAD_REQUEST),
    SAME_PASSWORD("error.same_password", 36, HttpStatus.UNPROCESSABLE_CONTENT),
    TOKEN_BLACKLISTED("error.token_blacklisted", 37, HttpStatus.UNAUTHORIZED);

    private static final Map<Integer, EApplicationError> BY_ERROR_CODE = new HashMap<>();

    static {
        for (EApplicationError e : values()) {
            BY_ERROR_CODE.put(e.errorCode, e);
        }
    }

    private final String messageKey;

    private final Integer errorCode;

    private final HttpStatus status;

    public static EApplicationError getByErrorCode(final Integer code) {
        if (code == null) {
            return null;
        }
        return BY_ERROR_CODE.get(code);
    }
}
