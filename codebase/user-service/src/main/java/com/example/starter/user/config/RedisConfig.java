package com.example.starter.user.config;

import com.example.starter.user.entity.LoginKeyEntity;
import com.example.starter.user.entity.RefreshTokenEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * <code>RedisConfig</code> 配置登入相關資料（refresh token、一次性登入金鑰）在 Redis 上使用的 {@link RedisTemplate}。
 * <br>
 * {@link RedisConnectionFactory} 由 Spring Boot 依 {@code spring.data.redis.*} 屬性值自動配置，這裡不需要再手動建立。
 */
@Configuration
public class RedisConfig {

    @Bean("redisRefreshTokenTemplate")
    public RedisTemplate<String, RefreshTokenEntity> redisRefreshTokenTemplate(RedisConnectionFactory connectionFactory) {
        return buildRedisTemplate(connectionFactory);
    }

    @Bean("redisLoginKeyTemplate")
    public RedisTemplate<String, LoginKeyEntity> redisLoginKeyTemplate(RedisConnectionFactory connectionFactory) {
        return buildRedisTemplate(connectionFactory);
    }

    private <T> RedisTemplate<String, T> buildRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
