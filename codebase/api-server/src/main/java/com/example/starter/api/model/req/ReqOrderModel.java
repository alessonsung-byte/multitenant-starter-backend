package com.example.starter.api.model.req;

import com.example.starter.business.order.model.OrderInput;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <code>ReqOrderModel</code> 新增訂單。沒有小計欄位：小計由後端以單價×數量計算，前端送來的金額一律不採用
 */
@Data
public class ReqOrderModel {

    private String customerId;

    private LocalDate orderDate;

    private String itemName;

    private BigDecimal unitPrice;

    private Integer quantity;

    public OrderInput toInput() {
        return new OrderInput(customerId, orderDate, itemName, unitPrice, quantity);
    }
}
