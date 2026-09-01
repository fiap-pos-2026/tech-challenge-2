package br.com.fiap.pos.tech_challenge.core.domain.exception;

/**
 * Semantic error status, free of any web framework type. The web layer maps the
 * {@link #code()} onto its own HTTP status representation.
 */
public enum ErrorStatus {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    UNPROCESSABLE_ENTITY(422),
    LOCKED(423),
    TOO_MANY_REQUESTS(429);

    private final int code;

    ErrorStatus(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
