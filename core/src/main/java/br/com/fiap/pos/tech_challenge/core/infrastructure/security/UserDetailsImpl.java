package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal. Composes a framework-free {@link User} domain model instead of
 * extending it, so the domain never depends on Spring Security. The former getter surface is
 * kept (delegating to {@link #user()}) so existing callers and tests are unaffected.
 */
@NullMarked
public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(final User user) {
        this.user = new User(user);
    }

    /** The underlying domain model. */
    public User user() {
        return user;
    }

    // ---- UserDetails ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getLogin();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    // ---- delegating accessors kept for existing callers ----

    public Long getId() {
        return user.getId();
    }

    public UUID getUuid() {
        return user.getUuid();
    }

    public String getLogin() {
        return user.getLogin();
    }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getPhone() {
        return user.getPhone();
    }

    public String getHash() {
        return user.getHash();
    }

    public UserRole getRole() {
        return user.getRole();
    }

    public boolean isActive() {
        return user.isActive();
    }

    public boolean isForceChangePassword() {
        return user.isForceChangePassword();
    }
}
