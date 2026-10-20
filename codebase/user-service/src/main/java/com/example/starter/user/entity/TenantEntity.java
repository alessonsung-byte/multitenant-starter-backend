package com.example.starter.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <code>TenantEntity</code> 對應資料表 mt_tenant：租戶（多租戶的租戶單位）。
 * <br>
 * 租戶本身不屬於任何一個租戶：所有交易都可以讀取（登入時要確認帳號所屬租戶是否啟用），新增、修改只有總管理員的交易可以（Row-Level Security，見 V2 migration）。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mt_tenant")
public class TenantEntity {

    /**
     * 租戶編號。資料庫自動產生 UUID 作為 PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * 租戶名稱，不可重複
     */
    @Column(name = "tenant_name")
    private String tenantName;

    /**
     * 是否啟用。停用後該租戶所有帳號立即無法使用（停用而非刪除）
     */
    @Column(name = "tenant_enabled")
    private Boolean tenantEnabled;

    /**
     * 新建資料的時間，值為毫秒
     */
    @Column(name = "create_time")
    private Long createTime;

    /**
     * 新建資料的使用者編號，即 mt_user.user_id
     */
    @Column(name = "create_user")
    private String createUser;

    /**
     * 更新資料的時間，值為毫秒
     */
    @Column(name = "update_time")
    private Long updateTime;

    /**
     * 更新資料的使用者編號，即 mt_user.user_id
     */
    @Column(name = "update_user")
    private String updateUser;
}
