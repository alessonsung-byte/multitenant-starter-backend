package com.example.starter.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

/**
 * <code>MessageSourceConfig</code> configures the component for {@link MessageSource}
 * <br>
 * {@link MessageSource} 元件的宣告之所以要脫離其他的 <code>@Configuration</code> 是為了避免循環依賴造成下列錯誤：
 * <pre>
 *     unresolvable circular reference
 * </pre>
 */
@Configuration
public class MessageSourceConfig {

    @Value("${locale.default.language}")
    private String defaultLocaleLanguage;

    @Value("${locale.default.country}")
    private String defaultLocaleCountry;

    /**
     * Define the component {@link MessageSource}
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setAlwaysUseMessageFormat(true);
        messageSource.setDefaultLocale(new Locale.Builder()
                .setLanguage(defaultLocaleLanguage)
                .setRegion(defaultLocaleCountry)
                .build());

        return messageSource;
    }
}
