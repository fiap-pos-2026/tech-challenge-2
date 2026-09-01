package br.com.fiap.pos.tech_challenge.core.infrastructure.security;

import br.com.fiap.pos.tech_challenge.core.application.port.out.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistPort {

    private static final String KEY_PREFIX = "blacklist:jti:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void invalidate(final String jti, final long ttlSeconds) {
        if (ttlSeconds <= 0) return;
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isBlacklisted(final String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
