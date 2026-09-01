package br.com.fiap.pos.tech_challenge.core.application.port.out;

/** Outbound port for JWT jti blacklisting (logout with immediate invalidation). */
public interface TokenBlacklistPort {
    void invalidate(String jti, long ttlSeconds);
    boolean isBlacklisted(String jti);
}
