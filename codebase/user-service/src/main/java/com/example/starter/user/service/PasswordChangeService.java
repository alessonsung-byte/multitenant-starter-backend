package com.example.starter.user.service;

import com.example.starter.common.exception.InvalidArgumentException;
import com.example.starter.common.exception.AppAuthenticationException;
import com.example.starter.common.service.EncryptHelper;
import com.example.starter.user.entity.LoginKeyEntity;
import com.example.starter.user.entity.SigninEntity;
import com.example.starter.user.entity.UserEntity;
import com.example.starter.user.repository.RedisLoginKeyRepository;
import com.example.starter.user.repository.SigninRepository;
import com.example.starter.user.repository.UserRepository;
import com.example.starter.user.audit.AuditAction;
import com.example.starter.user.audit.AuditRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.security.PrivateKey;

/**
 * <code>PasswordChangeService</code> 使用者自己修改密碼（{@code POST /api/me/password}）。
 * <br>
 * 密碼與登入一樣不以明碼傳送：前端先呼叫 {@code GET /api/public/publickey/{帳號}} 取得一次性公鑰與 nonce，
 * 用同一把隨機 AES 密鑰（以 nonce 當 IV）分別加密目前密碼與新密碼，再用公鑰加密這把 AES 密鑰（流程見 doc/HOW_TO_LOGIN.md）。
 * <br>
 * 改完後「需要修改密碼」的標記清除，使用者就可以使用其他功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final RedisLoginKeyRepository redisLoginKeyRepository;

    private final EncryptHelper encryptHelper;

    private final SigninRepository signinRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final PasswordPolicy passwordPolicy;

    private final AuditRecorder auditRecorder;

    /**
     * @param username                 目前登入的帳號
     * @param nonce                    取得公鑰時拿到的一次性標誌（也是 AES 的 IV）
     * @param encryption               用公鑰加密後的 AES 密鑰
     * @param encryptedCurrentPassword 用 AES 加密後的目前密碼
     * @param encryptedNewPassword     用 AES 加密後的新密碼
     */
    @Transactional
    public void changePassword(String username, String nonce, String encryption,
                               String encryptedCurrentPassword, String encryptedNewPassword) {
        // 取出同時刪除：這組 nonce／私鑰只能使用一次
        LoginKeyEntity loginKey = redisLoginKeyRepository.takeByNonce(nonce)
                .filter(key -> key.getUsername().equals(username))
                .orElseThrow(() -> new InvalidArgumentException("操作逾時，請重新送出"));

        String currentPassword;
        String newPassword;
        try {
            PrivateKey privateKey = encryptHelper.base64StringToPrivateKey(loginKey.getPrivateKeyBase64());
            SecretKey aesKey = encryptHelper.decryptAESKeyWithRSAPrivateKey(encryptHelper.base64Decode(encryption), privateKey);
            byte[] iv = encryptHelper.base64Decode(nonce);
            currentPassword = encryptHelper.toString(encryptHelper.decryptDataWithAES(aesKey, iv, encryptHelper.base64Decode(encryptedCurrentPassword)));
            newPassword = encryptHelper.toString(encryptHelper.decryptDataWithAES(aesKey, iv, encryptHelper.base64Decode(encryptedNewPassword)));
        } catch (Exception e) {
            log.info("Password change failed: cannot decrypt. username: {}", username);
            throw new InvalidArgumentException("密碼資料無法解讀，請重新送出");
        }

        SigninEntity signin = signinRepository.findBySigninAccount(username)
                .orElseThrow(() -> new AppAuthenticationException("請重新登入"));
        if (!passwordEncoder.matches(currentPassword, signin.getSigninPw())) {
            log.info("Password change failed: wrong current password. username: {}", username);
            throw new InvalidArgumentException("目前密碼錯誤");
        }
        passwordPolicy.validate(username, newPassword, signin.getSigninPw());

        long now = System.currentTimeMillis();
        signin.setSigninPw(passwordEncoder.encode(newPassword));
        signin.setUpdateTime(now);
        signin.setUpdateUser(signin.getUserId());

        UserEntity user = userRepository.findById(signin.getUserId())
                .orElseThrow(() -> new AppAuthenticationException("請重新登入"));
        user.setMustChangePassword(false);
        user.setUpdateTime(now);
        user.setUpdateUser(user.getUserId());

        // 不記錄任何密碼內容
        auditRecorder.record(user.getTenantId(), AuditAction.UPDATE, "mt_user", user.getUserId(), username,
                             AuditRecorder.snapshot("密碼", "（原密碼）"), AuditRecorder.snapshot("密碼", "（使用者自行修改）"));
        log.info("Password changed. username: {}", username);
    }
}
