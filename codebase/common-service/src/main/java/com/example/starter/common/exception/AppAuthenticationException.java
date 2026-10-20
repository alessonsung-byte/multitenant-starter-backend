package com.example.starter.common.exception;

import java.io.Serial;

/**
 * <code>AppAuthenticationException</code> 登入流程失敗（帳號密碼錯誤、登入逾時、refresh token 無效等）時丟出。
 * <br>
 * 由 api-server 的 {@code GlobalExceptionHandler} 統一轉成 401 回應；訊息內容會直接顯示給使用者，不可包含敏感資訊
 */
public class AppAuthenticationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3287659481725104623L;

    public AppAuthenticationException(String message) {
        super(message);
    }

    public AppAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
