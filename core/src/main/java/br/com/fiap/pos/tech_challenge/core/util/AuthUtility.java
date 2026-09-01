package br.com.fiap.pos.tech_challenge.core.util;

import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.UserDetailsImpl;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class AuthUtility {

    public UserDetailsImpl getLoggedUser() {
        try {
            return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception _) {
            throw new CoreException(EApplicationError.NO_AUTHENTICATION);
        }
    }
}
