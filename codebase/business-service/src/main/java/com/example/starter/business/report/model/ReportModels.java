package com.example.starter.business.report.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * <code>ReportModels</code> 報表的回應格式。報表只計入有效（未作廢）的訂單
 */
public final class ReportModels {

    private ReportModels() {
    }

    /**
     * 月報表的一列：一位客戶在這個月的訂單筆數與金額
     */
    public record CustomerRow(String customerId, String customerName, long orderCount, BigDecimal amount) {
    }

    /**
     * 一個租戶的月報表：依金額由大到小排列，最後附上合計
     *
     * @param month       月份（yyyy-MM）
     * @param rows        每位客戶一列；這個月沒有訂單的客戶不列出
     * @param totalCount  訂單筆數合計
     * @param totalAmount 金額合計
     */
    public record MonthlyReport(String month, List<CustomerRow> rows, long totalCount, BigDecimal totalAmount) {
    }

    /**
     * 跨租戶報表的一列：一個租戶在這個月的訂單筆數與金額（沒有訂單的租戶也列出，數字為 0）
     */
    public record TenantRow(String tenantId, String tenantName, long orderCount, BigDecimal amount) {
    }

    /**
     * 總管理員的跨租戶報表
     */
    public record TenantReport(String month, List<TenantRow> rows, long totalCount, BigDecimal totalAmount) {
    }
}
