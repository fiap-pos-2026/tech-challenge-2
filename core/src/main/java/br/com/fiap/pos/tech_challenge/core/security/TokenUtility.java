package br.com.fiap.pos.tech_challenge.core.security;

import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.mapper.TokenMapper;
import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenUtility {

    private final JwtEncoder encoder;

    private final JwtDecoder decoder;

    private final TokenMapper mapper;

    @Value("${jwt.expiry.duration}")
    private Duration expiryTime;

    public TokenDTO generate(final UserDetailsImpl impl) {
        var currentInstant = Instant.now();

        var builder = JwtClaimsSet.builder()
                .issuer("tech-challenge-app")
                .issuedAt(currentInstant)
                .expiresAt(currentInstant.plus(expiryTime))
                .subject(impl.getLogin())
                .claim("id", impl.getId())
                .claim("first_name", impl.getFirstName())
                .claim("last_name", impl.getLastName())
                .claim("e-mail", impl.getEmail())
                .claim("phone", impl.getPhone())
                .claim("hash", impl.getHash())
                .claim("force_change_password", impl.isForceChangePassword());

        if (impl.getUuid() != null) {
            builder.claim("uuid", impl.getUuid().toString());
        }
        if (impl.getRole() != null) {
            builder.claim("role", impl.getRole().name());
        }

        return mapper.toDTO(encoder.encode(JwtEncoderParameters.from(builder.build())));
    }

    public Jwt decode(final String token) {
        try {
            return decoder.decode(token);
        } catch (JwtException _) {
            throw new CoreException(EApplicationError.INVALID_TOKEN);
        }
    }

    public String getSubject(final String token) {
        return this.decode(token).getSubject();
    }

    public String getClaim(final String token, final String claim) {
        return this.decode(token).getClaimAsString(claim);
    }

    /**
     * Validates if the token actually belongs to the holder, avoiding that
     * a user who was deleted continue to use the same token if another
     * user was created with the same login.
     * @param token the JWT token sent in the request
     * @param details the loaded user details
     */
    public void validate(final String token, final UserDetailsImpl details) {
        final var hash = this.getClaim(token, "hash");
        if (!Strings.CS.equals(hash, details.getHash())) {
            log.error("Token do not belong to the holder");
            throw new CoreException(EApplicationError.INVALID_TOKEN);
        }
    }
}
