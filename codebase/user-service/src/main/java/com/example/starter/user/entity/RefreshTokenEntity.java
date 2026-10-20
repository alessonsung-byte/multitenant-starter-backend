package com.example.starter.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * <code>RefreshTokenEntity</code> 目前有效的 refresh token 紀錄，存放在 Redis，以 refresh token 的編號（JWT 的 jti）當 key。
 * <br>
 * 每次登入各自產生一筆，所以同一個帳號可以同時在電腦與手機登入，互不影響；
 * 登出時刪除這筆紀錄，已刪除的 refresh token 即使尚未過期也不能再使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 2371826402757240716L;

    /**
     * refresh token 的編號（JWT 的 jti）
     */
    private String tokenId;

    /**
     * 這個 refresh token 所屬的登入帳號
     */
    private String username;
}
