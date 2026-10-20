package com.example.starter.api.config;

import com.example.starter.common.exception.ConflictException;
import com.example.starter.common.exception.InvalidArgumentException;
import com.example.starter.common.exception.NotFoundException;
import com.example.starter.common.exception.AppAuthenticationException;
import com.example.starter.common.model.resp.RespErrorModel;
import com.example.starter.common.model.resp.RespMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * <code>GlobalExceptionHandler</code> 統一處理 controller 丟出的例外，讓所有錯誤回應都是 {@link RespErrorModel} 格式，
 * 與 {@link CustomAuthenticationEntryPoint}、{@link CustomAccessDeniedHandler} 一致
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 登入失敗、refresh token 無效等：401
     */
    @ExceptionHandler(AppAuthenticationException.class)
    public ResponseEntity<RespErrorModel> handleAuthenticationFailure(AppAuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new RespErrorModel(RespMessageCode.ERROR_USER_LOGIN, ex.getMessage()));
    }

    /**
     * 找不到資料（包含其他租戶的資料）：404
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<RespErrorModel> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RespErrorModel(RespMessageCode.ERROR_NOT_FOUND, ex.getMessage()));
    }

    /**
     * 與現有資料衝突（例如名稱重複、狀態不允許）：409
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<RespErrorModel> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespErrorModel(RespMessageCode.ERROR_CONFLICT, ex.getMessage()));
    }

    /**
     * 兩個人同時新增相同名稱等情況，由資料庫的唯一限制擋下：409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespErrorModel> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.info("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespErrorModel(RespMessageCode.ERROR_CONFLICT, "資料與現有資料衝突，請重新整理後再試"));
    }

    /**
     * 要修改或刪除的資料在交易中「消失」：被別人改過，或資料庫的 Row-Level Security 不允許這個帳號修改（例如總管理員）：409
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<RespErrorModel> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.info("Update or delete affected no rows: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RespErrorModel(RespMessageCode.ERROR_CONFLICT, "資料已被修改或沒有修改的權限，請重新整理後再試"));
    }

    /**
     * 資料庫存取錯誤。
     * <br>
     * 資料庫的 Row-Level Security 拒絕寫入（SQLState 42501，例如總管理員新增業務資料）時回 403，
     * 這代表程式層的權限檢查漏掉了、由資料庫擋下；其他資料庫錯誤一律 500，詳細內容只寫進日誌
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<RespErrorModel> handleDataAccess(DataAccessException ex) {
        if (isInsufficientPrivilege(ex)) {
            log.warn("Database rejected the operation by row-level security: {}", ex.getMostSpecificCause().getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RespErrorModel(RespMessageCode.ERROR_AUTHORIZATION_DENIED, "沒有執行此功能的權限"));
        }
        log.error("Database error.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RespErrorModel(RespMessageCode.ERROR_UNCATEGORIZED, "系統發生錯誤，請稍後再試"));
    }

    /**
     * 使用者輸入的內容不符合規則：400
     */
    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<RespErrorModel> handleInvalidArgument(InvalidArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespErrorModel(RespMessageCode.ERROR_INVALID_ARGUMENT, ex.getMessage()));
    }

    /**
     * {@code @PreAuthorize} 不通過（已登入但角色不符）：403。
     * 需要明確處理，否則會被下面的「其他錯誤」攔截成 500
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespErrorModel> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RespErrorModel(RespMessageCode.ERROR_AUTHORIZATION_DENIED, "沒有執行此功能的權限"));
    }

    /**
     * request 欄位驗證失敗（例如必填欄位是空的）：400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespErrorModel> handleValidationFailure(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespErrorModel(RespMessageCode.ERROR_INVALID_ARGUMENT, message));
    }

    /**
     * request 內容無法解析（例如 JSON 格式錯誤）：400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespErrorModel> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RespErrorModel(RespMessageCode.ERROR_INVALID_ARGUMENT, "request 內容格式錯誤"));
    }

    /**
     * 其他錯誤。
     * <br>
     * Spring MVC 本身的標準錯誤（例如網址不存在 404、HTTP method 不支援 405）沿用其原本的 HTTP 狀態碼；
     * 其餘未預期的錯誤一律回 500，詳細內容只寫進日誌，不回傳給前端
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespErrorModel> handleOthers(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.info("Request failed with status {}: {}", status.value(), ex.getMessage());
            return ResponseEntity.status(status)
                    .body(new RespErrorModel(RespMessageCode.ERROR_INVALID_ARGUMENT, "無效的請求（HTTP " + status.value() + "）"));
        }

        if (isInsufficientPrivilege(ex)) {
            log.warn("Database rejected the operation by row-level security: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new RespErrorModel(RespMessageCode.ERROR_AUTHORIZATION_DENIED, "沒有執行此功能的權限"));
        }

        log.error("Unexpected error.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RespErrorModel(RespMessageCode.ERROR_UNCATEGORIZED, "系統發生錯誤，請稍後再試"));
    }

    private static boolean isInsufficientPrivilege(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && "42501".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
