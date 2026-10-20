package com.example.starter.business.audit.model;

import com.example.starter.user.audit.AuditAction;

import java.util.Map;

/**
 * <code>AuditLogModel</code> 一筆操作紀錄（總管理員的操作紀錄頁）
 *
 * @param auditLogId      編號
 * @param time            操作時間（毫秒）
 * @param operatorId      操作者
 * @param operatorAccount 操作者帳號
 * @param tenantId        被操作的資料所屬租戶
 * @param tenantName      租戶名稱
 * @param action          動作
 * @param tableName       資料表
 * @param recordId        資料編號
 * @param recordLabel     方便辨認的名稱
 * @param changes         變更內容
 */
public record AuditLogModel(String auditLogId, Long time, String operatorId, String operatorAccount, String tenantId, String tenantName,
                            AuditAction action, String tableName, String recordId, String recordLabel, Map<String, Object> changes) {
}
