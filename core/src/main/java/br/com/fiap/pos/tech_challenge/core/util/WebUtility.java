package br.com.fiap.pos.tech_challenge.core.util;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@UtilityClass
public class WebUtility {

    public URI getLocation(final Object id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    public static void writeError(HttpServletResponse response, EApplicationError handledError, String message) throws IOException {
        response.setStatus(handledError.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(message, handledError.getErrorCode());

        byte[] bytes = new ObjectMapper().writeValueAsBytes(errorResponse);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
