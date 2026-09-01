package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.application.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class AuditLogFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);

        int status = response.getStatus();
        if (status == 401 || status == 403) {
            AuditEventType eventType = status == 401 ? AuditEventType.LOGIN_FAILED : AuditEventType.ACCESS_DENIED;
            String identifier = request.getRemoteAddr() + " " + request.getMethod() + " " + request.getRequestURI();
            try {
                auditLogService.register(eventType, null, identifier, String.valueOf(status), null);
            } catch (Exception ex) {
                log.warn("Falha ao registrar evento de auditoria para {}: {}", identifier, ex.getMessage());
            }
        }
    }
}
