package com.example.starter.business.audit.entity;

import com.example.starter.business.common.AuditedEntity;
import com.example.starter.user.audit.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * <code>AuditLogEntity</code> 對應資料表 mt_audit_log：操作紀錄。
 * <br>
 * 租戶欄位不使用 {@code @TenantId}：由寫入的 Service 明確指定；讀取只有總管理員（資料庫 Row-Level Security 限制）。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "mt_audit_log")
public class AuditLogEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_log_id")
    private String auditLogId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "operator_id")
    private String operatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_action")
    private AuditAction auditAction;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "record_id")
    private String recordId;

    @Column(name = "record_label")
    private String recordLabel;

    /**
     * 變更內容：{欄位: {from: 原本的值, to: 新的值}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes")
    private Map<String, Object> changes;
}
