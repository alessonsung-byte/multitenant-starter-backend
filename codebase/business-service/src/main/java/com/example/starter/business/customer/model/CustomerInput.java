package com.example.starter.business.customer.model;

/**
 * <code>CustomerInput</code> 新增、修改客戶的內容（必填與長度由 Service 檢查）
 *
 * @param customerName  客戶名稱（必填）
 * @param customerCode  客戶代碼（選填，同一租戶內不可重複）
 * @param customerEmail 聯絡信箱（選填）
 * @param active        是否往來中：只在修改時使用，null 表示不變更
 */
public record CustomerInput(String customerName, String customerCode, String customerEmail, Boolean active) {
}
