package br.com.fiap.pos.tech_challenge.core.security;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.util.Translator;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static br.com.fiap.pos.tech_challenge.core.enums.EApplicationError.INVALID_TOKEN;
import static br.com.fiap.pos.tech_challenge.core.enums.EApplicationError.TOKEN_EXPIRED;
import static java.util.Objects.requireNonNullElseGet;

@Component
@Log4j2
@RequiredArgsConstructor
public class AppAuthEntryPoint implements AuthenticationEntryPoint {

    private static final String EXPIRED_KEYWORD = "expired";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final Translator translator;

    @Override
    public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException exception) throws IOException {

        EApplicationError error = EApplicationError.TOKEN_NOT_SENT;

        if (exception instanceof InvalidBearerTokenException e) {
            error = e.getMessage().contains(EXPIRED_KEYWORD) ? TOKEN_EXPIRED : INVALID_TOKEN;
        }

        String clientIp = requireNonNullElseGet(request.getHeader(FORWARDED_FOR_HEADER), request::getRemoteAddr);

        log.error("[{}] {} from {} - {} | {}", request.getMethod(), request.getRequestURI(),
                clientIp, error.name(), exception.getMessage());

        WebUtility.writeError(response, error, translator.translateFromRequest(error.getMessageKey(), request));
    }
}
