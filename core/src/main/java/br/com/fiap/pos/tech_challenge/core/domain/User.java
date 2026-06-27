package br.com.fiap.pos.tech_challenge.core.domain;

import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "_id")
    private Long id;

    @Column(name = "_first_name", nullable = false)
    private String firstName;

    @Column(name = "_last_name", nullable = false)
    private String lastName;

    @Column(name = "_email", nullable = false, unique = true)
    private String email;

    @Column(name = "_birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "_login", nullable = false, unique = true)
    private String login;

    @Column(name = "_password", nullable = false)
    private String password;

    @Column(name = "_phone", nullable = false)
    private String phone;

    @Column(name = "_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "_last_login")
    private LocalDateTime lastLogin;

    @Column(name = "_hash", nullable = false)
    private String hash;

    @Column(name = "_uuid", unique = true)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "_role")
    private UserRole role;

    @Column(name = "_active", nullable = false)
    private boolean active = true;

    @Column(name = "_tentativas_login_falha", nullable = false)
    private int loginFailedAttempts = 0;

    @Column(name = "_bloqueado_ate")
    private LocalDateTime lockedUntil;

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
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        User user = (User) o;
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy p ?
                p.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.hash == null) {
            this.hash = UUID.randomUUID().toString();
        }
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID();
        }
    }
}
