package com.example.starter.api.model.res;

import lombok.Data;

import java.util.Set;

/**
 * <code>RespLoginModel</code> 登入或換發 access token 成功時的回應
 */
@Data
public class RespLoginModel {

    private String userId;

    /**
     * 登入帳號
     */
    private String username;

    private Set<String> roleNames;

    private Set<String> permissionNames;

    /**
     * 所屬租戶；總管理員為空
     */
    private String tenantId;

    /**
     * 所屬租戶名稱；總管理員為空
     */
    private String tenantName;

    /**
     * 是否需要先修改密碼。為 true 時，除了修改密碼與登出以外的 API 都會回 403
     */
    private boolean mustChangePassword;

    /**
     * 呼叫需要登入的 API 時，放在 HTTP header：{@code Authorization: Bearer <accessToken>}
     */
    private String accessToken;

    /**
     * access token 逾期時間（毫秒）
     */
    private Long expiredAt;

    /**
     * 用來換發新的 access token（{@code POST /api/public/refreshToken}）
     */
    private String refreshToken;
}
