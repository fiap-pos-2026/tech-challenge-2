package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import br.com.fiap.pos.tech_challenge.core.application.port.out.PasswordHasher;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author pauloogsouza
 * @since 2026-06-28
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class AdminCredentialInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SecureRandom random = new SecureRandom();

    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String configuredPassword;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByLogin("admin")
                .filter(User::isForceChangePassword)
                .ifPresent(this::applyInitialPassword);
    }

    private void applyInitialPassword(User admin) {
        String password = configuredPassword.isBlank()
                ? generateSecurePassword()
                : configuredPassword;

        admin.setPassword(passwordHasher.hash(password));
        userRepository.save(admin);

        log.warn("=================================================================");
        log.warn("  CREDENCIAL INICIAL DO ADMINISTRADOR");
        log.warn("  Login : admin");
        log.warn("  Senha : {}", password);
        log.warn("  ALTERE ESTA SENHA IMEDIATAMENTE APÓS O PRIMEIRO LOGIN.");
        log.warn("  Defina ADMIN_INITIAL_PASSWORD para controlar a senha inicial.");
        log.warn("=================================================================");
    }

    private String generateSecurePassword() {
        String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower   = "abcdefghijklmnopqrstuvwxyz";
        String digits  = "0123456789";
        String special = "@#$%&!*";
        String all     = upper + lower + digits + special;

        List<Character> chars = new ArrayList<>(16);

        chars.add(upper.charAt(random.nextInt(upper.length())));
        chars.add(upper.charAt(random.nextInt(upper.length())));
        chars.add(lower.charAt(random.nextInt(lower.length())));
        chars.add(lower.charAt(random.nextInt(lower.length())));
        chars.add(digits.charAt(random.nextInt(digits.length())));
        chars.add(digits.charAt(random.nextInt(digits.length())));
        chars.add(special.charAt(random.nextInt(special.length())));
        chars.add(special.charAt(random.nextInt(special.length())));

        for (int i = chars.size(); i < 16; i++) {
            chars.add(all.charAt(random.nextInt(all.length())));
        }

        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder(chars.size());
        chars.forEach(sb::append);
        return sb.toString();
    }
}
