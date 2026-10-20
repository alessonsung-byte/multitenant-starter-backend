package com.example.starter.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * <code>LoginAttemptService</code> 登入失敗次數限制，防止暴力猜密碼。
 * <br>
 * 以「登入帳號」為單位記錄在 Redis：
 * <ul>
 *     <li>連續失敗達到上限（預設 5 次）就鎖定一段時間（預設 15 分鐘），時間到自動解鎖</li>
 *     <li>登入成功、或總管理員重設密碼時，失敗次數歸零（立即解鎖）</li>
 *     <li>不存在的帳號也一樣計算與鎖定，外人無法從回應判斷帳號是否存在</li>
 * </ul>
 */
@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "LOGIN_FAILURE:";

    private final StringRedisTemplate redisTemplate;

    private final int maxFailures;

    private final Duration lockDuration;

    public LoginAttemptService(StringRedisTemplate redisTemplate,
                               @Value("${login.lock.max.failures}") int maxFailures,
                               @Value("${login.lock.duration.seconds}") long lockDurationInSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofSeconds(lockDurationInSeconds);
    }

    /**
     * 這個帳號目前是否被鎖定
     */
    public boolean isLocked(String account) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + account);
        return value != null && Long.parseLong(value) >= maxFailures;
    }

    /**
     * 記錄一次登入失敗。第一次失敗開始計時；達到上限時，從這一次起鎖定 {@code lockDuration}
     */
    public void recordFailure(String account) {
        String key = KEY_PREFIX + account;
        Long failures = redisTemplate.opsForValue().increment(key);
        if (failures != null && (failures == 1 || failures >= maxFailures)) {
            redisTemplate.expire(key, lockDuration);
        }
    }

    /**
     * 失敗次數歸零（登入成功、總管理員重設密碼時）
     */
    public void reset(String account) {
        redisTemplate.delete(KEY_PREFIX + account);
    }
}
