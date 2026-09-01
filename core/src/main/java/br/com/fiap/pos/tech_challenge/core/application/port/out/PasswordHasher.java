package br.com.fiap.pos.tech_challenge.core.application.port.out;

/** Outbound port for password hashing / verification. */
public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
