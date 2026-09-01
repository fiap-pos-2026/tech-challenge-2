package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.application.port.out.TokenBlacklistPort;
import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.application.AuditLogService;
import br.com.fiap.pos.tech_challenge.core.util.Translator;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    private final UserDetailsService service;

    private final Translator translator;

    private TokenUtility tokenUtility;

    private TokenBlacklistPort tokenBlacklistService;

    private AuditLogService auditLogService;

    private static final String BEARER_TOKEN = "Bearer";

    public JWTAuthorizationFilter(AuthenticationManager authenticationManager, UserDetailsService service,
                                   Translator translator) {
        super(authenticationManager);
        this.service = service;
        this.translator = translator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws IOException, ServletException {

        final String token = this.getToken(request);

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            UserDetails details = service.loadUserByUsername(tokenUtility.getSubject(token));

            tokenUtility.validate(token, (UserDetailsImpl) details);

            UserDetailsImpl impl = (UserDetailsImpl) details;

            if (isBlacklistedToken(token, impl)) {
                EApplicationError error = EApplicationError.TOKEN_BLACKLISTED;
                WebUtility.writeError(response, error, translator.translateFromRequest(error.getMessageKey(), request));
                return;
            }

            if (!impl.isActive()) {
                EApplicationError error = EApplicationError.ACCOUNT_INACTIVE;
                WebUtility.writeError(response, error, translator.translateFromRequest(error.getMessageKey(), request));
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (CoreException e) {
            var error = new AtomicReference<>(EApplicationError.getByErrorCode(e.getErrorCode()));

            if (EApplicationError.INVALID_USERNAME_PASSWORD.equals(error.get())) {
                error.set(EApplicationError.INVALID_TOKEN);
            }

            WebUtility.writeError(response, error.get(), translator.translateFromRequest(error.get().getMessageKey(), request));

            return;
        } catch (Exception e) {
            log.error(e.getMessage());

            EApplicationError error = EApplicationError.INVALID_TOKEN;
            WebUtility.writeError(response, error, translator.translateFromRequest(error.getMessageKey(), request));

            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isBlacklistedToken(String token, UserDetailsImpl impl) {
        try {
            String jti = tokenUtility.getJti(token);
            if (tokenBlacklistService == null || !tokenBlacklistService.isBlacklisted(jti)) {
                return false;
            }
            tryRegisterBlacklistAudit(impl, jti);
            return true;
        } catch (Exception e) {
            log.warn("Falha ao verificar blacklist (fail-open): {}", e.getMessage());
            return false;
        }
    }

    private void tryRegisterBlacklistAudit(UserDetailsImpl impl, String jti) {
        try {
            auditLogService.register(AuditEventType.BLACKLISTED_TOKEN_REJECTED,
                    impl.user(), jti, "BLOCKED", "Token on blacklist");
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de token na blacklist: {}", e.getMessage());
        }
    }

    private String getToken(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isNotBlank(authorization)) {
            return authorization.replace(BEARER_TOKEN, StringUtils.EMPTY).trim();
        }

        return null;
    }

    @Autowired
    public void setTokenUtility(TokenUtility tokenUtility) {
        this.tokenUtility = tokenUtility;
    }

    @Autowired
    public void setTokenBlacklistService(TokenBlacklistPort tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Autowired
    public void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}
