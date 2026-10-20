package com.example.starter.user.model;

/**
 * <code>AccountModel</code> 帳號資料（總管理員的帳號列表），不含任何密碼資訊
 *
 * @param userId             帳號編號（mt_user.user_id）
 * @param account            登入帳號
 * @param email              信箱，可為空
 * @param roleName           角色：SYS_ADMIN（總管理員）或 TENANT_USER（租戶帳號）
 * @param tenantId           所屬租戶；總管理員為空
 * @param tenantName         所屬租戶名稱；總管理員為空
 * @param enabled            帳號是否啟用
 * @param mustChangePassword 是否還沒改掉臨時密碼
 * @param createTime         建立時間（毫秒）
 */
public record AccountModel(String userId, String account, String email, String roleName, String tenantId,
                           String tenantName, boolean enabled, boolean mustChangePassword, Long createTime) {
}
