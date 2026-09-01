package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentActorAdapter implements CurrentActorPort {

    @Override
    public Optional<User> currentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl impl) {
                return Optional.of(impl.user());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
