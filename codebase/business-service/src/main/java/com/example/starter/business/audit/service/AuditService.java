package com.example.starter.business.audit.service;

import com.example.starter.business.audit.entity.AuditLogEntity;
import com.example.starter.business.audit.model.AuditLogModel;
import com.example.starter.business.audit.repository.AuditLogRepository;
import com.example.starter.business.common.PageModel;
import com.example.starter.business.common.PageRequestRule;
import com.example.starter.business.common.BusinessDate;
import com.example.starter.common.exception.InvalidArgumentException;
import com.example.starter.user.audit.AuditAction;
import com.example.starter.user.audit.AuditRecorder;
import com.example.starter.user.entity.TenantEntity;
import com.example.starter.user.entity.UserEntity;
import com.example.starter.user.repository.TenantRepository;
import com.example.starter.user.repository.UserRepository;
import com.example.starter.user.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <code>AuditService</code> 操作紀錄：寫入與總管理員查詢。
 * <br>
 * 寫入時只記錄「有變動的欄位」：新增記錄所有欄位的新值，刪除記錄所有欄位的原值（刪除前的內容），其他動作記錄改變的欄位。
 */
@Service
@RequiredArgsConstructor
public class AuditService implements AuditRecorder {

    private final AuditLogRepository auditLogRepository;

    private final UserRepository userRepository;

    private final TenantRepository tenantRepository;

    /**
     * 必須在業務操作的交易內呼叫（{@link Propagation#MANDATORY}）：業務資料寫入失敗時，操作紀錄也一起取消
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String tenantId, AuditAction action, String tableName, String recordId, String recordLabel,
                       Map<String, Object> before, Map<String, Object> after) {
        String operatorId = Objects.requireNonNullElse(TenantContext.current().userId(), "system");
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setOperatorId(operatorId);
        entity.setAuditAction(action);
        entity.setTableName(tableName);
        entity.setRecordId(recordId);
        entity.setRecordLabel(StringUtils.left(recordLabel, 256));
        entity.setChanges(changes(before, after));
        entity.markCreated(operatorId);
        auditLogRepository.save(entity);
    }

    /**
     * 有變動的欄位：{欄位: {from, to}}；新增只有 to、刪除只有 from
     */
    static Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        Set<String> fields = new HashSet<>();
        if (before != null) {
            fields.addAll(before.keySet());
        }
        if (after != null) {
            fields.addAll(after.keySet());
        }
        List<String> ordered = fields.stream().sorted().toList();
        for (String field : ordered) {
            Object from = before == null ? null : before.get(field);
            Object to = after == null ? null : after.get(field);
            if (before != null && after != null && Objects.equals(asText(from), asText(to))) {
                continue;
            }
            Map<String, Object> change = new LinkedHashMap<>();
            if (before != null) {
                change.put("from", asText(from));
            }
            if (after != null) {
                change.put("to", asText(to));
            }
            changes.put(field, change);
        }
        return changes;
    }

    /**
     * 值一律存成文字（日期、金額、true/false），讓畫面直接顯示
     */
    private static String asText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.math.BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    /**
     * 操作紀錄查詢（只有總管理員：資料庫只讓總管理員的交易讀取）
     *
     * @param tenantId 只看某一個租戶（選填）
     * @param from     操作日期起（含，台灣時間，選填）
     * @param to       操作日期迄（含，選填）
     */
    @Transactional(readOnly = true)
    public PageModel<AuditLogModel> search(String tenantId, LocalDate from, LocalDate to, Integer page, Integer size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidArgumentException("開始日期不可晚於結束日期");
        }
        Specification<AuditLogEntity> specification = Specification.where(null);
        if (StringUtils.isNotBlank(tenantId)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("tenantId"), tenantId));
        }
        if (from != null) {
            long fromMillis = from.atStartOfDay(BusinessDate.ZONE).toInstant().toEpochMilli();
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createTime"), fromMillis));
        }
        if (to != null) {
            long toMillis = to.plusDays(1).atStartOfDay(BusinessDate.ZONE).toInstant().toEpochMilli();
            specification = specification.and((root, query, builder) -> builder.lessThan(root.get("createTime"), toMillis));
        }
        Page<AuditLogEntity> result = auditLogRepository.findAll(specification, PageRequestRule.of(page, size,
                Sort.by(Sort.Order.desc("createTime"), Sort.Order.asc("auditLogId"))));

        Map<String, String> accounts = userRepository.findAllById(
                        result.getContent().stream().map(AuditLogEntity::getOperatorId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getUserAccount));
        Map<String, String> tenants = tenantRepository.findAllById(
                        result.getContent().stream().map(AuditLogEntity::getTenantId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(TenantEntity::getTenantId, TenantEntity::getTenantName));
        List<AuditLogModel> items = result.getContent().stream().map(entity -> new AuditLogModel(
                entity.getAuditLogId(), entity.getCreateTime(), entity.getOperatorId(), accounts.get(entity.getOperatorId()),
                entity.getTenantId(), entity.getTenantId() == null ? null : tenants.get(entity.getTenantId()), entity.getAuditAction(),
                entity.getTableName(), entity.getRecordId(), entity.getRecordLabel(), entity.getChanges())).toList();
        return new PageModel<>(items, result.getTotalElements());
    }
}
