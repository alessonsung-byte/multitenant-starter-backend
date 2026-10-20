package com.example.starter.business.order.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <code>OrderInput</code> 新增訂單的內容。沒有小計欄位：小計一律由後端以單價×數量計算
 *
 * @param customerId 客戶（必須是本租戶、往來中的客戶）
 * @param orderDate  訂單日期（不可晚於今天）
 * @param itemName   品項名稱
 * @param unitPrice  單價（0 以上、最多兩位小數）
 * @param quantity   數量（1～999）
 */
public record OrderInput(String customerId, LocalDate orderDate, String itemName, BigDecimal unitPrice, Integer quantity) {
}
