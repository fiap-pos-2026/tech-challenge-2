package br.com.fiap.pos.tech_challenge.core.security;

import br.com.fiap.pos.tech_challenge.core.domain.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NullMarked
public class UserDetailsImpl extends User implements UserDetails {

    public UserDetailsImpl(final User user) {
        super(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (getRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRole().name()));
    }

    @Override
    public String getUsername() {
        return super.getLogin();
    }
}
