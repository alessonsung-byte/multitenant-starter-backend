package com.example.starter.user.model;

import com.example.starter.user.entity.TenantEntity;

/**
 * <code>TenantModel</code> 租戶資料（總管理員的租戶列表、新增、修改的回應）
 *
 * @param tenantId      租戶編號
 * @param tenantName    租戶名稱
 * @param tenantEnabled 是否啟用
 * @param createTime    建立時間（毫秒）
 */
public record TenantModel(String tenantId, String tenantName, boolean tenantEnabled, Long createTime) {

    public static TenantModel from(TenantEntity entity) {
        return new TenantModel(entity.getTenantId(), entity.getTenantName(),
                               Boolean.TRUE.equals(entity.getTenantEnabled()), entity.getCreateTime());
    }
}
