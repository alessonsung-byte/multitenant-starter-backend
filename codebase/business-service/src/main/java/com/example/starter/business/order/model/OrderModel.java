package com.example.starter.business.order.model;

import com.example.starter.business.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <code>OrderModel</code> 一筆訂單（含客戶名稱，訂單查詢畫面使用）
 *
 * @param orderId      訂單編號
 * @param tenantId     所屬租戶
 * @param customerId   客戶
 * @param customerName 客戶名稱
 * @param orderDate    訂單日期
 * @param itemName     品項名稱
 * @param unitPrice    單價
 * @param quantity     數量
 * @param subtotal     小計
 * @param orderStatus  狀態
 * @param voidReason   作廢原因
 * @param voidTime     作廢時間（毫秒）
 * @param createTime   登打時間（毫秒）
 */
public record OrderModel(String orderId, String tenantId, String customerId, String customerName, LocalDate orderDate, String itemName,
                         BigDecimal unitPrice, int quantity, BigDecimal subtotal, OrderStatus orderStatus, String voidReason,
                         Long voidTime, Long createTime) {
}
