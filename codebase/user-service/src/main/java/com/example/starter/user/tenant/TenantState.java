package com.example.starter.user.tenant;

/**
 * <code>TenantState</code> 一個請求的租戶狀態（見 {@link TenantContext}）
 *
 * @param userId     登入者的 mt_user.user_id；尚未登入時為 null
 * @param tenantId   目前租戶。租戶帳號為自己的租戶；總管理員與尚未登入時為 null
 * @param allTenants 是否可以讀取所有租戶的資料（只有總管理員是 true，而且只能讀、不能寫業務資料）
 */
public record TenantState(String userId, String tenantId, boolean allTenants) {

    /**
     * 沒有任何租戶：查不到任何屬於租戶的資料
     */
    public static TenantState none() {
        return new TenantState(null, null, false);
    }

    /**
     * 租戶帳號：只能存取自己租戶的資料
     */
    public static TenantState tenant(String userId, String tenantId) {
        return new TenantState(userId, tenantId, false);
    }

    /**
     * 總管理員：可以讀取所有租戶的資料，但不能新增、修改、刪除業務資料
     */
    public static TenantState allTenants(String userId) {
        return new TenantState(userId, null, true);
    }
}
