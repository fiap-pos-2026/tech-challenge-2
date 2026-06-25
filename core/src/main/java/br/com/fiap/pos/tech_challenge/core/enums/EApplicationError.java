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
    INVALID_PRINCIPAL("error.invalid_principal", 11, HttpStatus.BAD_REQUEST);

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
