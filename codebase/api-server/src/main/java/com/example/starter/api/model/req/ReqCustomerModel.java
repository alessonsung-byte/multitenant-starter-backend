package com.example.starter.api.model.req;

import com.example.starter.business.customer.model.CustomerInput;
import lombok.Data;

/**
 * <code>ReqCustomerModel</code> 新增、修改客戶。必填與長度由 Service 檢查（錯誤訊息才能指出是哪個欄位）
 */
@Data
public class ReqCustomerModel {

    private String customerName;

    private String customerCode;

    private String customerEmail;

    /**
     * 是否往來中（只在修改時使用）：不傳（null）表示不變更
     */
    private Boolean active;

    public CustomerInput toInput() {
        return new CustomerInput(customerName, customerCode, customerEmail, active);
    }
}
