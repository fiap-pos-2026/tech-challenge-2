package br.com.fiap.pos.tech_challenge.core.infrastructure.config;

import br.com.fiap.pos.tech_challenge.core.infrastructure.security.AppAuthEntryPoint;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.AuditLogFilter;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.JWTAuthorizationFilter;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.PasswordChangeRequiredFilter;
import br.com.fiap.pos.tech_challenge.core.util.Translator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.public.key}")
    private RSAPublicKey publicKey;

    @Value("${jwt.private.key}")
    private RSAPrivateKey privateKey;

    @Qualifier("springUserDetailsService")
    private final UserDetailsService details;

    private final AuthenticationConfiguration configuration;

    private final AppAuthEntryPoint appAuthEntryPoint;

    private final AuditLogFilter auditLogFilter;

    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;

    private final Translator translator;

    private static final String[] IGNORED_ROUTES = {
            "/webjars/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/signin/**",
            "/error/**",
            "/eureka/**"
    };

    private static final String[] PUBLIC_OTP_ROUTES = {
            "/api/service-orders/*/status",
            "/api/service-orders/*/approval",
            "/api/service-orders/*/delivery/accept",
            "/api/service-orders/*/delivery/reject"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(it -> it
                        .requestMatchers(IGNORED_ROUTES).permitAll()
                        .requestMatchers(PUBLIC_OTP_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilter(authorizationFilter())
                .addFilterAfter(auditLogFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(passwordChangeRequiredFilter, BasicAuthenticationFilter.class)
                .exceptionHandling(Customizer.withDefaults())
                .oauth2ResourceServer(it -> it
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        .authenticationEntryPoint(appAuthEntryPoint))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        var rsaKey = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
        var jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JWTAuthorizationFilter authorizationFilter() {
        return new JWTAuthorizationFilter(configuration.getAuthenticationManager(), details, translator);
    }
}
