package com.example.starter.business.audit.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>AuditServiceChangesTest</code> 操作紀錄只記錄「有變動的欄位」：新增只有新值、刪除只有原值、修改只有改變的欄位
 */
class AuditServiceChangesTest {

    @Test
    void createRecordsOnlyNewValues() {
        Map<String, Object> changes = AuditService.changes(null, Map.of("名稱", "A"));

        assertThat(changes).containsOnlyKeys("名稱");
        assertThat(field(changes, "名稱")).containsOnlyKeys("to");
    }

    @Test
    void deleteRecordsOnlyOldValues() {
        Map<String, Object> changes = AuditService.changes(Map.of("名稱", "A"), null);

        assertThat(field(changes, "名稱")).containsOnlyKeys("from");
    }

    @Test
    void updateSkipsUnchangedFieldsAndComparesNumbersByValue() {
        Map<String, Object> changes = AuditService.changes(Map.of("名稱", "A", "單價", new BigDecimal("100.00")),
                                                           Map.of("名稱", "B", "單價", new BigDecimal("100")));

        assertThat(changes).containsOnlyKeys("名稱");
        assertThat(field(changes, "名稱")).containsEntry("from", "A").containsEntry("to", "B");
    }

    /**
     * 一個欄位的變更內容 {from, to}
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> field(Map<String, Object> changes, String name) {
        return (Map<String, Object>) changes.get(name);
    }
}
