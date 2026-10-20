package com.example.starter.user.service;

import com.example.starter.common.exception.InvalidArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * <code>PasswordPolicy</code> 密碼規則：
 * <ul>
 *     <li>至少 8 個字元</li>
 *     <li>不可與帳號相同（不分大小寫）</li>
 *     <li>不可與舊密碼相同</li>
 * </ul>
 * 另外，密碼以 BCrypt 雜湊，BCrypt 只能處理 72 bytes 以內的密碼，所以超過的長度也不接受。
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    /**
     * BCrypt 能處理的最大長度（UTF-8 編碼後的 bytes）
     */
    public static final int MAX_BYTES = 72;

    private final PasswordEncoder passwordEncoder;

    /**
     * 檢查新密碼是否符合規則，不符合時丟出 {@link InvalidArgumentException}（訊息可以直接顯示給使用者）
     *
     * @param account             登入帳號
     * @param newPassword         新密碼（明碼）
     * @param currentPasswordHash 目前密碼的雜湊值；沒有舊密碼時（例如產生臨時密碼）傳 null
     */
    public void validate(String account, String newPassword, String currentPasswordHash) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH) {
            throw new InvalidArgumentException("密碼至少需要 " + MIN_LENGTH + " 個字元");
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidArgumentException("密碼太長，請改用 72 個英數字元以內的密碼");
        }
        if (account != null && newPassword.equalsIgnoreCase(account)) {
            throw new InvalidArgumentException("密碼不可與帳號相同");
        }
        if (currentPasswordHash != null && passwordEncoder.matches(newPassword, currentPasswordHash)) {
            throw new InvalidArgumentException("新密碼不可與舊密碼相同");
        }
    }
}
