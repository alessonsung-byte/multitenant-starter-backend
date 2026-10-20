package com.example.starter.api.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/**
 * <code>ReqLoginModel</code> 登入的 request。四個欄位都是 Base64 編碼後的字串
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
public class ReqLoginModel {

    /**
     * 登入帳號，必須與取得公鑰時的帳號相同
     */
    @ToString.Include
    @NotBlank(message = "{login.username.empty}")
    private String username;

    /**
     * 用 AES 密鑰（以 nonce 當 IV）加密後的密碼
     */
    @NotBlank(message = "{login.password.empty}")
    private String password;

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
}
