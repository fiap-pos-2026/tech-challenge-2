package br.com.fiap.pos.tech_challenge.core.security;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static br.com.fiap.pos.tech_challenge.core.enums.EApplicationError.INVALID_TOKEN;
import static br.com.fiap.pos.tech_challenge.core.enums.EApplicationError.TOKEN_EXPIRED;

@Component
@Log4j2
public class AppAuthEntryPoint implements AuthenticationEntryPoint {

    private static final String EXPIRED_KEYWORD = "expired";

    @Override
    public void commence(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        log.error(exception.getMessage());

        AtomicReference<EApplicationError> error = new AtomicReference<>(EApplicationError.TOKEN_NOT_SENT);

        if (exception instanceof InvalidBearerTokenException e) {
            error.set(e.getMessage().contains(EXPIRED_KEYWORD) ? TOKEN_EXPIRED : INVALID_TOKEN);
        }

        EApplicationError handledError = error.get();

        WebUtility.writeError(response, handledError);
    }
}
