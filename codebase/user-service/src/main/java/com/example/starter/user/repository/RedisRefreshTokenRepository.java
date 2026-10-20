package com.example.starter.user.repository;

import com.example.starter.user.entity.RefreshTokenEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * <code>RedisRefreshTokenRepository</code> 管理 {@link RefreshTokenEntity} 在 Redis 上的存取。
 * <br>
 * 每一筆各自設定存活時間，與 refresh token 本身的有效期間相同，逾期後 Redis 自動刪除。
 * <br>
 * 另外以帳號為單位記錄「這個帳號有哪些 refresh token」，總管理員重設密碼、停用帳號時可以一次全部作廢。
 */
@Repository
public class RedisRefreshTokenRepository {

    private static final String KEY_PREFIX = "REFRESH_TOKEN:";

    private static final String USER_KEY_PREFIX = "USER_REFRESH_TOKENS:";

    private final RedisTemplate<String, RefreshTokenEntity> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final Duration timeToLive;

    public RedisRefreshTokenRepository(RedisTemplate<String, RefreshTokenEntity> redisRefreshTokenTemplate,
                                       StringRedisTemplate stringRedisTemplate,
                                       @Value("${user.jwt.refresh.token.expired.millis}") long refreshTokenExpiredInMillis) {
        this.redisTemplate = redisRefreshTokenTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.timeToLive = Duration.ofMillis(refreshTokenExpiredInMillis);
    }

    public void save(RefreshTokenEntity entity) {
        redisTemplate.opsForValue().set(KEY_PREFIX + entity.getTokenId(), entity, timeToLive);

        String userKey = USER_KEY_PREFIX + entity.getUsername();
        stringRedisTemplate.opsForSet().add(userKey, entity.getTokenId());
        stringRedisTemplate.expire(userKey, timeToLive);
    }

    /**
     * 作廢指定帳號的所有 refresh token（總管理員重設密碼、停用帳號時使用）
     */
    public void deleteAllByUsername(String username) {
        String userKey = USER_KEY_PREFIX + username;
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(userKey);
        if (tokenIds != null) {
            tokenIds.forEach(tokenId -> redisTemplate.delete(KEY_PREFIX + tokenId));
        }
        stringRedisTemplate.delete(userKey);
    }

    public Optional<RefreshTokenEntity> findByTokenId(String tokenId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + tokenId));
    }

    /**
     * 作廢指定編號的 refresh token（登出時使用）
     */
    public void deleteByTokenId(String tokenId) {
        redisTemplate.delete(KEY_PREFIX + tokenId);
    }
}
