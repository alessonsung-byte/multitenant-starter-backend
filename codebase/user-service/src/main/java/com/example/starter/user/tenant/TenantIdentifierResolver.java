package com.example.starter.user.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * <code>TenantIdentifierResolver</code> 告訴 Hibernate「目前是哪個租戶」（第一層：程式層）。
 * <br>
 * 屬於租戶的資料表，Entity 的租戶欄位標註 {@code @TenantId} 後：
 * <ul>
 *     <li>查詢時 Hibernate 自動加上「租戶 = 目前租戶」</li>
 *     <li>新增資料時 Hibernate 自動填入目前租戶，Service 不需要、也不可以自己設定</li>
 * </ul>
 * 總管理員時 {@link #isRoot(String)} 回傳 true，Hibernate 不加租戶條件（可以檢視所有租戶；寫入仍會被資料庫擋下）。
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    /**
     * 尚未登入或沒有租戶時使用的值；不會與任何真正的租戶編號相同，所以查不到任何資料
     */
    public static final String NO_TENANT = "NO_TENANT";

    /**
     * 租戶帳號：自己的租戶（新增資料時自動填入）。其他情況：{@link #NO_TENANT}
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.current().tenantId();
        return tenantId != null ? tenantId : NO_TENANT;
    }

    /**
     * 回傳 false：Hibernate 不檢查「交易進行中途租戶是否改變」，每個交易開始時由 {@link TenantContext} 決定
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(String tenantId) {
        return TenantContext.current().allTenants();
    }
}
