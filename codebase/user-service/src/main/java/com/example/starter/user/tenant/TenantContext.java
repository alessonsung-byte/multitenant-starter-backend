package com.example.starter.user.tenant;

/**
 * <code>TenantContext</code> 記錄「這個請求是誰、可以存取哪個租戶的資料」。
 * <br>
 * 每個請求各自一份（存在執行緒上），由 {@link com.example.starter.user.config.JwtAuthenticationFilter} 在驗證登入後設定、
 * 請求結束時清除。之後：
 * <ul>
 *     <li>{@link TenantIdentifierResolver} 讓 Hibernate 自動在查詢加上「租戶 = 目前租戶」</li>
 *     <li>{@link TenantAwareJpaTransactionManager} 在每個交易開始時把目前租戶告訴資料庫，由 Row-Level Security 再擋一層</li>
 * </ul>
 * 租戶 id 只從登入者推導，不接受前端傳入。沒有設定時（例如尚未登入）一律視為「沒有任何租戶」，查不到任何租戶的資料。
 */
public final class TenantContext {

    private static final ThreadLocal<TenantState> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * 目前請求的租戶狀態；沒有設定時回傳 {@link TenantState#none()}
     */
    public static TenantState current() {
        TenantState state = CURRENT.get();
        return state != null ? state : TenantState.none();
    }

    public static void set(TenantState state) {
        CURRENT.set(state);
    }

    /**
     * 請求結束時一定要清除，避免同一個執行緒處理下一個請求時沿用上一位使用者的租戶
     */
    public static void clear() {
        CURRENT.remove();
    }
}
