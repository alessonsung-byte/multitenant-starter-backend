package com.example.starter.user.config;

import com.example.starter.user.service.AppUserDetailsService;
import com.example.starter.user.service.UserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * <code>UserDetailsServiceConfig</code> configures the component for {@link UserDetailsService}
 * <br>
 * {@link UserDetailsService} 元件的宣告之所以要脫離其他的 <code>@Configuration</code> 是為了避免循環依賴造成下列錯誤：
 * <pre>
 *     unresolvable circular reference
 * </pre>
 */
@Configuration
@RequiredArgsConstructor
public class UserDetailsServiceConfig {

    private final UserHelper userHelper;

    @Bean
    public UserDetailsService userDetailsService() {
        return new AppUserDetailsService(userHelper);
    }
}
