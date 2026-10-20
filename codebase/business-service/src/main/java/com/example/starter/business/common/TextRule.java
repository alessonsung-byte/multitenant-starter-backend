package com.example.starter.business.common;

import com.example.starter.common.exception.InvalidArgumentException;
import org.apache.commons.lang3.StringUtils;

/**
 * <code>TextRule</code> 整理使用者輸入的文字：去除前後空白、空白字串視為沒有填、檢查長度
 */
public final class TextRule {

    private TextRule() {
    }

    /**
     * 選填：去除前後空白，沒有填時回傳 null
     *
     * @param label     欄位名稱，用在錯誤訊息（例如「客戶名稱」）
     * @param maxLength 最多幾個字（與資料表欄位長度相同）
     */
    public static String optional(String value, String label, int maxLength) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed != null && trimmed.length() > maxLength) {
            throw new InvalidArgumentException(label + "最多 " + maxLength + " 個字");
        }
        return trimmed;
    }

    /**
     * 必填：去除前後空白，沒有填時丟出「請輸入○○」
     */
    public static String required(String value, String label, int maxLength) {
        String trimmed = optional(value, label, maxLength);
        if (trimmed == null) {
            throw new InvalidArgumentException("請輸入" + label);
        }
        return trimmed;
    }

    /**
     * 模糊搜尋用：轉小寫並跳脫 LIKE 的特殊字元（% _ \）
     */
    public static String likePattern(String keyword) {
        String escaped = keyword.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
