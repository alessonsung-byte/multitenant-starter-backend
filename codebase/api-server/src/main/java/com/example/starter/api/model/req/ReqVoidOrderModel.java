package com.example.starter.api.model.req;

import lombok.Data;

/**
 * <code>ReqVoidOrderModel</code> 作廢訂單：原因必填（由 Service 檢查）
 */
@Data
public class ReqVoidOrderModel {

    private String reason;
}
