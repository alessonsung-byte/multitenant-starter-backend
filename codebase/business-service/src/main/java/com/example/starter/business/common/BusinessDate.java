package com.example.starter.business.common;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * <code>BusinessDate</code> 業務日期（訂單日期、報表月份）一律以同一個時區為準，不受伺服器時區影響。
 * <br>
 * 範例使用台灣時間；部署到其他地區時只需要改這裡。
 */
public final class BusinessDate {

    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");

    private BusinessDate() {
    }

    /**
     * 業務時區現在的日期
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
