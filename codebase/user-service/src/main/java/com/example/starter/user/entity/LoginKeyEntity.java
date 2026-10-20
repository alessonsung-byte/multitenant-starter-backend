package com.example.starter.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * <code>LoginKeyEntity</code> 登入流程用的一次性 RSA 私鑰，存放在 Redis，以 nonce 當 key。
 * <br>
 * 登入流程：
 * <ol>
 *     <li>前端呼叫 {@code GET /api/public/publickey/{username}}，伺服器產生一組 RSA 金鑰對與 nonce，
 *     私鑰存到這裡，公鑰與 nonce 回傳給前端</li>
 *     <li>前端產生一把隨機 AES 密鑰，用它（以 nonce 當 IV）加密密碼，再用公鑰加密這把 AES 密鑰，呼叫 {@code POST /api/public/login}</li>
 *     <li>伺服器依 nonce 找到私鑰，解出 AES 密鑰、再解出密碼後驗證；這筆資料不論成功與否都立即刪除（只能用一次，防止重放攻擊）</li>
 * </ol>
 */
@Data
@ToString(exclude = "privateKeyBase64")
@NoArgsConstructor
@AllArgsConstructor
public class LoginKeyEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 6188542672684351023L;

    /**
     * 一次性標誌，Base64 編碼後的值
     */
    private String nonce;

    /**
     * 取得公鑰時指定的登入帳號；登入時送來的帳號必須與此相同
     */
    private String username;

    /**
     * RSA 私鑰，Base64 編碼後的值
     */
    private String privateKeyBase64;
}
