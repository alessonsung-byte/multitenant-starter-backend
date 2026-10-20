package com.example.starter.user.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * <code>TenantConfig</code> 啟用多租戶的兩層保護：
 * <ol>
 *     <li>把 {@link TenantIdentifierResolver} 註冊給 Hibernate（程式層：自動加租戶條件）</li>
 *     <li>以 {@link TenantAwareJpaTransactionManager} 取代 Spring Boot 預設的交易管理器（資料庫層：每個交易設定目前租戶）</li>
 * </ol>
 */
@Configuration
public class TenantConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(TenantIdentifierResolver tenantIdentifierResolver) {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
    }

    /**
     * Bean 名稱必須是 transactionManager：{@code @Transactional} 與 Spring Data 的 repository 預設都使用這個名稱
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new TenantAwareJpaTransactionManager(entityManagerFactory);
    }
}
