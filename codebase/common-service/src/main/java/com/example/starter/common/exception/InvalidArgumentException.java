package com.example.starter.common.exception;

import java.io.Serial;

/**
 * <code>InvalidArgumentException</code> 使用者輸入的內容不符合規則時丟出（例如密碼太短、目前密碼錯誤），
 * 由 api-server 的 {@code GlobalExceptionHandler} 轉成 400。訊息會直接顯示給使用者
 */
public class InvalidArgumentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3361795304526384917L;

    public InvalidArgumentException(String message) {
        super(message);
    }
}
