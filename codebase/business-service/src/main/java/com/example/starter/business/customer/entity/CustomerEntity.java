package com.example.starter.business.customer.entity;

import com.example.starter.business.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

/**
 * <code>CustomerEntity</code> 對應資料表 mt_customer：客戶（屬於租戶）
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mt_customer")
public class CustomerEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_id")
    private String customerId;

    /**
     * 所屬租戶。由 Hibernate 依登入者自動填入與過濾，程式不可以自己設定
     */
    @TenantId
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "customer_name")
    private String customerName;

    /**
     * 客戶代碼，選填；同一租戶內不可重複
     */
    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "customer_email")
    private String customerEmail;

    /**
     * 是否往來中；false 為已停止往來（列表預設不顯示）
     */
    @Column(name = "customer_active")
    private boolean customerActive = true;
}
