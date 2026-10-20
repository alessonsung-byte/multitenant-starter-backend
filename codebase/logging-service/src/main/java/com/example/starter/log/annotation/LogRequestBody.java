package com.example.starter.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <code>LogRequestBody</code> 標記此註解後，日誌才會記錄 request body 的內容（預設一律不記錄）。
 * <br>
 * 標記在 controller class 上，表示此 class 底下所有 method 都記錄；標記在單一 method 上，只套用在該 method。
 * <br>
 * 為什麼預設不記錄：業務系統的 API 大多會傳送客戶資料、訂單金額等個人或商業資料，
 * 若預設記錄，只要有一支新 API 忘記排除，個資就會被寫進日誌檔。因此改成「要記錄的 API 才明確標記」。
 * <br>
 * 下列情況<b>不要</b>標記此註解：含有密碼、token、個人資料、訂單金額等敏感資訊；上傳檔案（multipart）；內容特別大的請求。
 *
 * @see com.example.starter.log.advice.RequestBodyLoggingHandler
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRequestBody {
}
