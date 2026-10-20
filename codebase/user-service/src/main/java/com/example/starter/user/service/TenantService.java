package com.example.starter.user.service;

import com.example.starter.common.exception.ConflictException;
import com.example.starter.common.exception.NotFoundException;
import com.example.starter.user.entity.TenantEntity;
import com.example.starter.user.model.TenantModel;
import com.example.starter.user.repository.TenantRepository;
import com.example.starter.user.audit.AuditAction;
import com.example.starter.user.audit.AuditRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>TenantService</code> 總管理員管理租戶：列表、新增、改名、啟用／停用。
 * <br>
 * 租戶只能停用、不能刪除。停用後該租戶所有帳號的下一次請求就被視為未登入
 * （{@link com.example.starter.user.model.AppUserDetails#isEnabled()}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    private final AuditRecorder auditRecorder;

    @Transactional(readOnly = true)
    public List<TenantModel> findAll() {
        return tenantRepository.findAllByOrderByCreateTimeAsc().stream().map(TenantModel::from).toList();
    }

    /**
     * 新增租戶，名稱不可與其他租戶相同
     *
     * @param tenantName 租戶名稱（前後空白會去除）
     * @param operatorId 操作者的 mt_user.user_id
     */
    @Transactional
    public TenantModel create(String tenantName, String operatorId) {
        return create(tenantName, operatorId, Map.of());
    }

    /**
     * 新增租戶，名稱不可與其他租戶相同；操作紀錄另外記錄 {@code extraAuditFields}
     */
    @Transactional
    public TenantModel create(String tenantName, String operatorId, Map<String, Object> extraAuditFields) {
        String name = tenantName.trim();
        if (tenantRepository.existsByTenantName(name)) {
            throw new ConflictException("租戶名稱「" + name + "」已經存在");
        }

        TenantEntity entity = new TenantEntity();
        entity.setTenantName(name);
        entity.setTenantEnabled(true);
        entity.setCreateTime(System.currentTimeMillis());
        entity.setCreateUser(operatorId);
        TenantEntity saved = tenantRepository.save(entity);
        Map<String, Object> snapshot = new LinkedHashMap<>(AuditRecorder.snapshot("租戶名稱", name));
        snapshot.putAll(extraAuditFields);
        auditRecorder.record(null, AuditAction.CREATE, "mt_tenant", saved.getTenantId(), name, null, snapshot);
        log.info("Tenant created by {}: {}", operatorId, name);
        return TenantModel.from(saved);
    }

    /**
     * 修改租戶名稱
     */
    @Transactional
    public TenantModel rename(String tenantId, String tenantName, String operatorId) {
        TenantEntity entity = findEntity(tenantId);
        String name = tenantName.trim();
        if (tenantRepository.existsByTenantNameAndTenantIdNot(name, tenantId)) {
            throw new ConflictException("租戶名稱「" + name + "」已經存在");
        }

        String oldName = entity.getTenantName();
        entity.setTenantName(name);
        touch(entity, operatorId);
        auditRecorder.record(null, AuditAction.UPDATE, "mt_tenant", tenantId, name,
                             AuditRecorder.snapshot("租戶名稱", oldName), AuditRecorder.snapshot("租戶名稱", name));
        return TenantModel.from(entity);
    }

    /**
     * 啟用或停用租戶
     */
    @Transactional
    public TenantModel setEnabled(String tenantId, boolean enabled, String operatorId) {
        TenantEntity entity = findEntity(tenantId);
        entity.setTenantEnabled(enabled);
        touch(entity, operatorId);
        auditRecorder.record(null, enabled ? AuditAction.ENABLE : AuditAction.DISABLE, "mt_tenant", tenantId, entity.getTenantName(),
                             AuditRecorder.snapshot("啟用", !enabled), AuditRecorder.snapshot("啟用", enabled));
        log.info("Tenant {} by {}: {}", enabled ? "enabled" : "disabled", operatorId, tenantId);
        return TenantModel.from(entity);
    }

    private TenantEntity findEntity(String tenantId) {
        return tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("找不到這個租戶"));
    }

    private void touch(TenantEntity entity, String operatorId) {
        entity.setUpdateTime(System.currentTimeMillis());
        entity.setUpdateUser(operatorId);
    }
}
