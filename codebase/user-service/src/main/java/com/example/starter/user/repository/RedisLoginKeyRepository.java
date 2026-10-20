package com.example.starter.user.repository;

import com.example.starter.user.entity.LoginKeyEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * <code>RedisLoginKeyRepository</code> 管理登入流程用的一次性 {@link LoginKeyEntity} 在 Redis 上的存取。
 * <br>
 * 每一筆各自設定存活時間（{@code login.nonce.valid.period.in.minutes}），逾期後 Redis 自動刪除。
 */
@Repository
public class RedisLoginKeyRepository {

    private static final String KEY_PREFIX = "LOGIN_KEY:";

    private final RedisTemplate<String, LoginKeyEntity> redisTemplate;

    private final Duration timeToLive;

    public RedisLoginKeyRepository(RedisTemplate<String, LoginKeyEntity> redisLoginKeyTemplate,
                                   @Value("${login.nonce.valid.period.in.minutes}") long nonceValidPeriodInMinutes) {
        this.redisTemplate = redisLoginKeyTemplate;
        this.timeToLive = Duration.ofMinutes(nonceValidPeriodInMinutes);
    }

    public void save(LoginKeyEntity entity) {
        redisTemplate.opsForValue().set(KEY_PREFIX + entity.getNonce(), entity, timeToLive);
    }

    /**
     * 取出並同時刪除指定 nonce 的資料，確保同一組 nonce／私鑰只能使用一次
     */
    public Optional<LoginKeyEntity> takeByNonce(String nonce) {
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + nonce));
    }
}
