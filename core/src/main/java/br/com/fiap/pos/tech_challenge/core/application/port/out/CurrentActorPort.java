package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.User;

import java.util.Optional;

/** Outbound port exposing the authenticated actor without the application core touching Spring Security. */
public interface CurrentActorPort {
    Optional<User> currentUser();
}
