package com.example.starter.common.exception;

import java.io.Serial;

/**
 * <code>NotFoundException</code> 找不到要操作的資料時丟出，由 api-server 的 {@code GlobalExceptionHandler} 轉成 404。
 * <br>
 * 存取其他租戶的資料也一律丟出這個例外（不回 403），避免透露「這筆資料存在」。訊息會直接顯示給使用者
 */
public class NotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6519326046174729580L;

    public NotFoundException(String message) {
        super(message);
    }
}
