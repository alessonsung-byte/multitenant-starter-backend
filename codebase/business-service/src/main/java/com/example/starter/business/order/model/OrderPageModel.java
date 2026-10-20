package com.example.starter.business.order.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * <code>OrderPageModel</code> 訂單查詢的結果：這一頁的資料，加上「符合條件的全部有效訂單」的筆數與金額合計
 *
 * @param items       這一頁的資料（依條件可能包含作廢的）
 * @param total       符合條件的總筆數（含作廢的，如果有勾選顯示作廢）
 * @param validCount  符合條件的有效（未作廢）訂單筆數
 * @param validAmount 符合條件的有效訂單金額合計
 */
public record OrderPageModel(List<OrderModel> items, long total, long validCount, BigDecimal validAmount) {
}
