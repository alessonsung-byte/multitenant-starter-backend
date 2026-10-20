package com.example.starter.common.exception;

import java.io.Serial;

/**
 * <code>ConflictException</code> 與現有資料衝突時丟出（例如帳號重複、目前狀態不允許這個操作），
 * 由 api-server 的 {@code GlobalExceptionHandler} 轉成 409。訊息會直接顯示給使用者
 */
public class ConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1904378260954818420L;

    public ConflictException(String message) {
        super(message);
    }
}
