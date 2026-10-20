package com.example.starter.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <code>NoLogToHeaderAndParam</code> 標記在 controller class 上，該 class 所有 API 的日誌都不記錄 request headers 與 parameters；標記在單一 method 上，只套用在該 method。
 * <br>
 * 用於 request headers 與 parameters 含有敏感資料（個人資料、密碼等）的 API。
 *
 * @see com.example.starter.log.config.LoggingInterceptor
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoLogToHeaderAndParam {
}
