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

    INVALID_USERNAME_PASSWORD("Invalid login or password", 1, HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("Email already exists", 2, HttpStatus.CONFLICT),
    LOGIN_ALREADY_EXISTS("Login already exists", 3, HttpStatus.CONFLICT),
    INVALID_FIELDS("Invalid fields", 4, HttpStatus.BAD_REQUEST),
    MISSING_FIELDS("Missing fields", 5, HttpStatus.BAD_REQUEST),
    TOKEN_NOT_SENT("Unauthorized", 6, HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Unauthorized - invalid session", 7, HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Unauthorized - invalid token", 8, HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("User not found", 9, HttpStatus.NOT_FOUND),
    NO_AUTHENTICATION("No active authentication found", 10, HttpStatus.BAD_REQUEST),
    INVALID_PRINCIPAL("Authentication failed", 11, HttpStatus.BAD_REQUEST);

    private static final Map<Integer, EApplicationError> BY_ERROR_CODE = new HashMap<>();

    static {
        for (EApplicationError e : values()) {
            BY_ERROR_CODE.put(e.errorCode, e);
        }
    }

    private final String message;

    private final Integer errorCode;

    private final HttpStatus status;

    public static EApplicationError getByErrorCode(final Integer code) {
        if (code == null) {
            return null;
        }
        return BY_ERROR_CODE.get(code);
    }
}
