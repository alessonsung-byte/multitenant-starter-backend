package com.example.starter.business.customer.model;

import com.example.starter.business.customer.entity.CustomerEntity;

/**
 * <code>CustomerModel</code> 客戶資料
 *
 * @param customerId    客戶編號
 * @param tenantId      所屬租戶
 * @param customerName  客戶名稱
 * @param customerCode  客戶代碼；沒有時為 null
 * @param customerEmail 聯絡信箱；沒有時為 null
 * @param active        是否往來中
 * @param createTime    建立時間（毫秒）
 */
public record CustomerModel(String customerId, String tenantId, String customerName, String customerCode, String customerEmail,
                            boolean active, Long createTime) {

    public static CustomerModel from(CustomerEntity entity) {
        return new CustomerModel(entity.getCustomerId(), entity.getTenantId(), entity.getCustomerName(), entity.getCustomerCode(),
                                 entity.getCustomerEmail(), entity.isCustomerActive(), entity.getCreateTime());
    }
}
