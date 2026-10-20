package com.example.starter.business.common;

import java.util.List;

/**
 * <code>PageModel</code> 分頁查詢的回應：這一頁的資料與符合條件的總筆數
 *
 * @param items 這一頁的資料
 * @param total 符合條件的總筆數
 */
public record PageModel<T>(List<T> items, long total) {
}
