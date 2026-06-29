package br.com.fiap.pos.tech_challenge.core.security;

public interface TokenBlacklistService {

    void invalidate(String jti, long ttlSeconds);

    boolean isBlacklisted(String jti);
}
