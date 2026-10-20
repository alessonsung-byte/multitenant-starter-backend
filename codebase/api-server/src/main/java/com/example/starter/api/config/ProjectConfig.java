package com.example.starter.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.starter.user.config.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * <code>ProjectConfig</code> 設定 API 的安全機制：哪些 API 不用登入、如何驗證登入、錯誤時回傳什麼。
 * <br>
 * 登入後的每次請求都由 {@link JwtAuthenticationFilter} 檢查 access token；不使用 HTTP session。
 * <br>
 * 不開放跨網域（CORS）：前端的 Next.js 伺服器在伺服器端呼叫本服務，瀏覽器不會直接呼叫本服務。
 * <br>
 * {@code @EnableMethodSecurity}：之後可以在 controller 使用 {@code @PreAuthorize} 限制角色或權限
 */
@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class ProjectConfig {

    private final MessageSource messageSource;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(management -> management.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(request -> request
                        // 不需要登入：登入相關 API、服務狀態檢查
                        .requestMatchers("/api/public/**", "/ping", "/actuator/health").permitAll()
                        // 總管理員功能：整組限制 SYS_ADMIN（controller 另外以 @PreAuthorize 再限制一次）
                        .requestMatchers("/api/admin/**").hasRole("SYS_ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // 未登入或 token 無效：401
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        // 已登入但權限不足：403
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 需要修改密碼的帳號只能呼叫「修改密碼」
                .addFilterAfter(new PasswordChangeRequiredFilter(objectMapper), JwtAuthenticationFilter.class);
        // @formatter:on

        return http.build();
    }

    /**
     * 讓 request 欄位驗證（{@code @Valid}）的錯誤訊息使用 messages*.properties 的多語系文字
     */
    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }
}
