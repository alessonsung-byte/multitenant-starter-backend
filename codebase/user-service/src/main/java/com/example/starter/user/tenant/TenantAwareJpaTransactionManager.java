package com.example.starter.user.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serial;

/**
 * <code>TenantAwareJpaTransactionManager</code> 每個資料庫交易開始後，立刻把「目前租戶」告訴 PostgreSQL
 * （第二層：資料庫層）。
 * <br>
 * 資料庫的 Row-Level Security 規則讀取這兩個設定，決定這個交易看得到、寫得進哪些資料：
 * <ul>
 *     <li>{@code app.tenant_id}：目前租戶；只能讀寫這個租戶的資料</li>
 *     <li>{@code app.all_tenants}：{@code on} 表示可以讀取所有租戶（總管理員），但不能寫入</li>
 * </ul>
 * {@code set_config} 第三個參數 {@code true} 表示只在這個交易有效，連線還給連線池後不會留給下一個請求。
 * 沒有設定時值是空字串，規則一律不成立，所以「設定失敗」的結果是看不到資料，而不是看到別人的資料。
 * <br>
 * 所有資料存取都必須在 {@code @Transactional} 內進行；不在交易內的查詢因為沒有設定，查不到任何屬於租戶的資料。
 */
public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    @Serial
    private static final long serialVersionUID = 1385512407713356172L;

    private static final String SET_TENANT_SQL = "SELECT set_config('app.tenant_id', :tenantId, true), "
            + "set_config('app.all_tenants', :allTenants, true)";

    public TenantAwareJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);

        EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
        TenantState state = TenantContext.current();
        try {
            holder.getEntityManager().createNativeQuery(SET_TENANT_SQL)
                    .setParameter("tenantId", nullToEmpty(state.tenantId()))
                    .setParameter("allTenants", state.allTenants() ? "on" : "off")
                    .getSingleResult();
        } catch (RuntimeException ex) {
            // 設定失敗時不能繼續使用這個交易：取消交易並歸還連線
            try {
                holder.getEntityManager().getTransaction().rollback();
            } finally {
                doCleanupAfterCompletion(transaction);
            }
            throw new CannotCreateTransactionException("無法設定目前租戶", ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
