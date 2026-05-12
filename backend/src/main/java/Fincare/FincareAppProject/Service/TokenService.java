package Fincare.FincareAppProject.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_PREFIX = "refresh:";

    public void blacklistAccessToken(String token, long ttlMs) {
        if (ttlMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttlMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    public void storeRefreshToken(String username, String token) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + username, token, refreshExpirationMs, TimeUnit.MILLISECONDS);
    }

    public boolean isRefreshTokenValid(String username, String token) {
        String stored = redisTemplate.opsForValue().get(REFRESH_PREFIX + username);
        return token.equals(stored);
    }

    public void deleteRefreshToken(String username) {
        redisTemplate.delete(REFRESH_PREFIX + username);
    }
}
