package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.LoginDTO;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.security.TokenUtility;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthenticationService {

    private final TokenUtility tokenUtility;

    private final AuthenticationConfiguration configuration;

    private final UserService userService;

    public TokenDTO authenticate(final LoginDTO dto) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(dto.getLogin(),
                dto.getPassword(), Collections.emptyList());

            AuthenticationManager manager = configuration.getAuthenticationManager();

            Authentication authentication = manager.authenticate(authToken);

            UserDetailsImpl impl = ((UserDetailsImpl) authentication.getPrincipal());

            if (impl == null) {
                throw new CoreException(EApplicationError.INVALID_PRINCIPAL);
            }

            TokenDTO token = tokenUtility.generate(impl);

            userService.updateLastLogin(impl.getId());

            return token;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CoreException(EApplicationError.INVALID_USERNAME_PASSWORD);
        }
    }
}
