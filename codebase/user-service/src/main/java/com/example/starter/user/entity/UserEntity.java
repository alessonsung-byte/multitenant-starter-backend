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
 * <code>UserEntity</code> 對應資料表 mt_user：使用者基本資料。
 * <br>
 * 登入用的帳號密碼另外存在 mt_signin（{@link SigninEntity}），兩者以 user_id 關聯。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mt_user")
public class UserEntity {

    /**
     * 使用者編號。資料庫自動產生 UUID 作為 PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String userId;

    /**
     * 使用者帳號（顯示用），與 mt_signin.signin_account 相同，不可重複
     */
    @Column(name = "user_account")
    private String userAccount;

    /**
     * 使用者信箱，可為空
     */
    @Column(name = "user_email")
    private String userEmail;

    /**
     * 使用者是否啟用。停用的帳號無法登入（停用而非刪除）
     */
    @Column(name = "user_enabled")
    private Boolean userEnabled;

    /**
     * 所屬租戶，即 mt_tenant.tenant_id。租戶帳號必填；總管理員為空
     */
    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * 是否需要修改密碼。新帳號、被總管理員重設密碼後為 true，使用者改完密碼後為 false
     */
    @Column(name = "must_change_password")
    private Boolean mustChangePassword;

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
