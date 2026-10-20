package com.example.starter.api.model.res;

import lombok.Data;

/**
 * <code>RespPublicKeyModel</code> 登入第 1 步的回應
 */
@Data
public class RespPublicKeyModel {

    private String username;

    /**
     * 這次登入專用的 RSA 公鑰（X.509 格式，Base64 編碼）
     */
    private String publicKey;

    /**
     * 一次性標誌（16 bytes，Base64 編碼），同時作為 AES 加密密碼時的 IV
     */
    private String nonce;
}
