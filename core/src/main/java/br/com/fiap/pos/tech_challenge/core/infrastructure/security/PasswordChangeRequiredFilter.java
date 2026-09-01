package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.util.Translator;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Bloqueia todas as rotas autenticadas quando force_change_password=true,
 * exceto os endpoints de perfil necessários para a troca de senha.
 *
 * @author johncgo
 * @since 2026-06-27
 */
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    // Rotas liberadas enquanto o usuário ainda não trocou a senha
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/profile/password",  // PUT — troca de senha
            "/api/profile",           // GET — dados do usuário logado (necessário para UI)
            "/api/signout"            // POST — logout sempre acessível (FR-011)
    );

    private final Translator translator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserDetailsImpl principal
                && principal.isForceChangePassword()
                && isBlocked(request.getRequestURI())) {

            EApplicationError error = EApplicationError.PASSWORD_CHANGE_REQUIRED;
            WebUtility.writeError(response, error,
                    translator.translateFromRequest(error.getMessageKey(), request));
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isBlocked(String uri) {
        return ALLOWED_PATHS.stream().noneMatch(uri::endsWith);
    }
}
