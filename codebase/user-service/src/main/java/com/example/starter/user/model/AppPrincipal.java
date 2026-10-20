package com.example.starter.user.model;

import java.security.Principal;

/**
 * <code>AppPrincipal</code> 登入後放在 Spring Security 中的登入者資訊（不含密碼），
 * 由 {@link com.example.starter.user.config.JwtAuthenticationFilter} 每次請求重新讀取資料庫後建立。
 * <br>
 * Controller 可以用 {@code @AuthenticationPrincipal AppPrincipal principal} 取得；
 * {@code Authentication.getName()} 仍然是登入帳號。
 *
 * @param userId             mt_user.user_id
 * @param username           登入帳號
 * @param tenantId           所屬租戶；總管理員為 null
 * @param sysAdmin           是否為總管理員
 * @param mustChangePassword 是否需要先修改密碼
 */
public record AppPrincipal(String userId, String username, String tenantId, boolean sysAdmin,
                              boolean mustChangePassword) implements Principal {

    public static AppPrincipal from(AppUserDetails userDetails) {
        return new AppPrincipal(userDetails.getUser().getUserId(), userDetails.getUsername(), userDetails.getTenantId(),
                                   userDetails.isSysAdmin(), userDetails.isMustChangePassword());
    }

    @Override
    public String getName() {
        return username;
    }
}
