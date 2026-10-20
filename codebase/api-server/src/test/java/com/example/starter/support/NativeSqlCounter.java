package com.example.starter.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * <code>NativeSqlCounter</code> 以手寫 SQL（不經過 Hibernate 的租戶條件）計算資料表的筆數（只在測試中使用）。
 * <br>
 * 透過後端正式的交易管理器執行，用來證明：就算程式漏加租戶條件，資料庫的 Row-Level Security 仍然只回傳目前租戶的資料。
 */
@Service
public class NativeSqlCounter {

    /**
     * 只允許這些資料表（表名直接放進 SQL，所以限制可用的名稱）
     */
    private static final Set<String> TABLES = Set.of("mt_customer", "mt_sales_order");

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public long count(String table) {
        if (!TABLES.contains(table)) {
            throw new IllegalArgumentException("不允許的資料表：" + table);
        }
        return ((Number) entityManager.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult()).longValue();
    }
}
