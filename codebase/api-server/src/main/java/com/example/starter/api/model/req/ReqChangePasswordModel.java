package com.example.starter.api.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/**
 * <code>ReqChangePasswordModel</code> 修改密碼的 request。四個欄位都是 Base64 編碼後的字串，
 * 密碼以登入相同的方式加密（先呼叫 {@code GET /api/public/publickey/{帳號}} 取得公鑰與 nonce）
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class ReqChangePasswordModel {

    /**
     * 取得公鑰時拿到的一次性標誌，同時是 AES 加密密碼時的 IV
     */
    @NotBlank(message = "{login.nonce.empty}")
    private String nonce;

    /**
     * 用公鑰加密後的 AES 密鑰
     */
    @NotBlank(message = "{login.encryption.empty}")
    private String encryption;

    /**
     * 用 AES 密鑰加密後的目前密碼
     */
    @NotBlank(message = "{password.current.empty}")
    private String currentPassword;

    /**
     * 用同一把 AES 密鑰加密後的新密碼
     */
    @NotBlank(message = "{password.new.empty}")
    private String newPassword;
}
