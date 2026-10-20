package com.example.starter.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * <code>LocaleConfig</code> 設定回應訊息（例如錯誤訊息）要使用的語系。
 * <br>
 * 依照前端送來的 HTTP header {@code Accept-Language} 決定語系；沒有帶這個 header 時，使用設定檔中的預設語系。
 * <br>
 * 使用 {@link AcceptHeaderLocaleResolver} 而不是 {@code SessionLocaleResolver}：本系統是 JWT 無狀態（stateless）API，
 * 不使用 HTTP session，語系不應該存放在 session 裡。
 */
@Configuration
public class LocaleConfig {

    @Value("${locale.default.language}")
    private String defaultLocaleLanguage;

    @Value("${locale.default.country}")
    private String defaultLocaleCountry;

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(new Locale.Builder()
                .setLanguage(defaultLocaleLanguage)
                .setRegion(defaultLocaleCountry)
                .build());
        return localeResolver;
    }
}
