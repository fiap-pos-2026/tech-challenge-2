package br.com.fiap.pos.tech_challenge.core.domain.model;

import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private LocalDate birthday;

    private String login;

    private String password;

    private String phone;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    private String hash;

    private UUID uuid;

    private UserRole role;

    private boolean active = true;

    private int loginFailedAttempts = 0;

    private LocalDateTime lockedUntil;

    private boolean forceChangePassword = false;

    public User(User that) {
        this.id = that.id;
        this.firstName = that.firstName;
        this.lastName = that.lastName;
        this.email = that.email;
        this.birthday = that.birthday;
        this.login = that.login;
        this.password = that.password;
        this.phone = that.phone;
        this.createdAt = that.createdAt;
        this.lastLogin = that.lastLogin;
        this.hash = that.hash;
        this.uuid = that.uuid;
        this.role = that.role;
        this.active = that.active;
        this.loginFailedAttempts = that.loginFailedAttempts;
        this.lockedUntil = that.lockedUntil;
        this.forceChangePassword = that.forceChangePassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return uuid != null && java.util.Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
