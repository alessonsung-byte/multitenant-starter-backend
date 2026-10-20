package com.example.starter.common.model.resp;

import lombok.Getter;

/**
 * <code>RespMessageCode</code> API 錯誤回應的錯誤碼（見 {@link RespErrorModel}）。新增錯誤碼時，號碼不可重複
 */
@Getter
public enum RespMessageCode {

    /**
     * 登入失敗、refresh token 無效（401）
     */
    ERROR_USER_LOGIN("1000"),

    /**
     * request 欄位驗證失敗（400）
     */
    ERROR_INVALID_ARGUMENT("1009"),

    /**
     * 已登入但權限不足（403）
     */
    ERROR_AUTHORIZATION_DENIED("1014"),

    /**
     * 未登入，或 access token 無效／過期（401）
     */
    ERROR_UNAUTHORIZED("1015"),

    /*
     * 1016、1018 已作廢，不可重複使用。新增的錯誤碼從 1020 開始
     */

    /**
     * 找不到資料（404）。存取其他租戶的資料也一律回這個，不透露資料是否存在
     */
    ERROR_NOT_FOUND("1020"),

    /**
     * 與現有資料衝突（409），例如帳號或名稱重複、目前狀態不允許這個操作
     */
    ERROR_CONFLICT("1021"),

    /**
     * 需要先修改密碼（403）：新帳號第一次登入、或被總管理員重設密碼後，除了修改密碼與登出以外的功能都不能使用
     */
    ERROR_PASSWORD_CHANGE_REQUIRED("1022"),

    /**
     * 其他未預期的錯誤（500）
     */
    ERROR_UNCATEGORIZED("9999");

    private final String code;

    RespMessageCode(String code) {
        this.code = code;
    }
}
