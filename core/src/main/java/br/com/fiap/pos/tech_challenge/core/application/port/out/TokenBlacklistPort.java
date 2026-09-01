package br.com.fiap.pos.tech_challenge.core.application.port.out;

public interface TokenBlacklistPort {
    void invalidate(String jti, long ttlSeconds);
    boolean isBlacklisted(String jti);
}
