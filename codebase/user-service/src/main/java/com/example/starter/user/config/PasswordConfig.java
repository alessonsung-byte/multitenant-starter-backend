package com.example.starter.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Value("${bcrypt.password.encoder.strength}")
    private int encodeStrength;

    /**
     * 密碼以 BCrypt 雜湊後儲存；強度（cost）由 {@code bcrypt.password.encoder.strength} 設定。
     * <br>
     * BCrypt 的雜湊值本身會記錄當初使用的強度，所以日後調整強度，舊密碼仍然可以正常驗證，只影響之後新設定的密碼
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(encodeStrength);
    }
}
