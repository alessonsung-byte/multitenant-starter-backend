package com.example.starter.user.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <code>AuditRecorder</code> 寫入操作紀錄。
 * <br>
 * 由 tenant-service 的 {@code AuditService} 實作；放在 user-service 是因為租戶、帳號的操作也要記錄，
 * 而 user-service 不能依賴 tenant-service。
 * <br>
 * 必須在業務操作的同一個交易內呼叫：業務資料寫入失敗時，操作紀錄也會一起取消。
 */
public interface AuditRecorder {

    /**
     * 記錄一次操作
     *
     * @param tenantId    被操作的資料所屬租戶；租戶、帳號、全域設定為 null
     * @param action      動作
     * @param tableName   資料表，例如 mt_customer
     * @param recordId    資料編號
     * @param recordLabel 方便辨認的名稱（例如客戶名稱）
     * @param before      操作前的內容（新增時為 null）；只放可以給總管理員看的欄位，不放密碼
     * @param after       操作後的內容（刪除時為 null）
     */
    void record(String tenantId, AuditAction action, String tableName, String recordId, String recordLabel,
                Map<String, Object> before, Map<String, Object> after);

    /**
     * 建立「操作前／操作後」內容：依序傳入欄位名稱與值（值可以是 null），保持欄位順序
     */
    static Map<String, Object> snapshot(Object... namesAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            map.put((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return map;
    }
}
