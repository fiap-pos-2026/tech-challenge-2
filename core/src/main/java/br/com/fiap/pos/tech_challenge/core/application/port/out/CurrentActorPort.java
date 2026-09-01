package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.User;

import java.util.Optional;

public interface CurrentActorPort {
    Optional<User> currentUser();
}
