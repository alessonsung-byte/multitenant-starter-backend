package com.example.starter.user.model;

/**
 * <code>CurrentUserModel</code> 目前登入者的資訊（{@code GET /api/me}）
 *
 * @param userId             帳號編號（mt_user.user_id）
 * @param username           登入帳號
 * @param roleName           角色：SYS_ADMIN（總管理員）或 TENANT_USER（租戶帳號）
 * @param tenantId           所屬租戶；總管理員為空
 * @param tenantName         所屬租戶名稱；總管理員為空
 * @param mustChangePassword 是否需要先修改密碼
 */
public record CurrentUserModel(String userId, String username, String roleName, String tenantId, String tenantName,
                               boolean mustChangePassword) {

    public static CurrentUserModel from(AppUserDetails userDetails) {
        String roleName = userDetails.isSysAdmin() ? AppUserDetails.ROLE_SYS_ADMIN
                : userDetails.getRoles().stream().findFirst().orElse(null);
        String tenantName = userDetails.getTenant() != null ? userDetails.getTenant().getTenantName() : null;
        return new CurrentUserModel(userDetails.getUser().getUserId(), userDetails.getUsername(), roleName,
                                    userDetails.getTenantId(), tenantName, userDetails.isMustChangePassword());
    }
}
