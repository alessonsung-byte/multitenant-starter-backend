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
 * <code>SigninEntity</code> 對應資料表 mt_signin：使用者登入用的帳號與密碼雜湊值。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mt_signin")
public class SigninEntity {

    /**
     * 登入資料編號。資料庫自動產生 UUID 作為 PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "signin_id")
    private String signinId;

    /**
     * 登入帳號，不可重複
     */
    @Column(name = "signin_account")
    private String signinAccount;

    /**
     * 登入密碼的 BCrypt 雜湊值（不儲存明碼）
     */
    @Column(name = "signin_pw")
    private String signinPw;

    /**
     * 對應的使用者編號，即 mt_user.user_id
     */
    @Column(name = "user_id")
    private String userId;

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
