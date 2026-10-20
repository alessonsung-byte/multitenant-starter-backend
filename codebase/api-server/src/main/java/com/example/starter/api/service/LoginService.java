package com.example.starter.api.service;

import com.example.starter.api.model.req.ReqLoginModel;
import com.example.starter.api.model.res.RespLoginModel;
import com.example.starter.api.model.res.RespPublicKeyModel;
import com.example.starter.common.exception.AppAuthenticationException;
import com.example.starter.common.service.EncryptHelper;
import com.example.starter.user.entity.LoginKeyEntity;
import com.example.starter.user.entity.RefreshTokenEntity;
import com.example.starter.user.model.AppUserDetails;
import com.example.starter.user.repository.RedisLoginKeyRepository;
import com.example.starter.user.repository.RedisRefreshTokenRepository;
import com.example.starter.user.service.JwtHelper;
import com.example.starter.user.service.LoginAttemptService;
import com.example.starter.user.service.UserHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Date;

/**
 * <code>LoginService</code> 登入相關服務：取得一次性公鑰、登入、換發 access token、登出。
 * <br>
 * 帳號不存在、密碼錯誤、帳號被鎖定一律回覆相同的訊息，避免外人藉由錯誤訊息猜出哪些帳號存在。
 * <br>
 * 同一個帳號連續登入失敗達到上限就鎖定一段時間（{@link LoginAttemptService}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    public static final String LOGIN_FAILED_MESSAGE = "帳號或密碼錯誤";

    private static final int NONCE_LENGTH_IN_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    private final EncryptHelper encryptHelper;

    private final UserHelper userHelper;

    private final PasswordEncoder passwordEncoder;

    private final JwtHelper jwtHelper;

    private final RedisLoginKeyRepository redisLoginKeyRepository;

    private final RedisRefreshTokenRepository redisRefreshTokenRepository;

    private final LoginAttemptService loginAttemptService;

    /**
     * 登入第 1 步：產生一組只給這次登入使用的 RSA 金鑰對與 nonce。
     * 私鑰存到 Redis，公鑰與 nonce 回傳給前端。
     * <br>
     * 不論帳號是否存在都正常回傳，避免外人藉此確認帳號是否存在。
     */
    public RespPublicKeyModel findPublicKey(String username) {
        try {
            KeyPair keyPair = encryptHelper.generateRSAKeyPair();
            String nonce = generateNonce();

            redisLoginKeyRepository.save(new LoginKeyEntity(
                    nonce, username, encryptHelper.privateKeyToBase64String(keyPair.getPrivate())));

            RespPublicKeyModel respModel = new RespPublicKeyModel();
            respModel.setUsername(username);
            respModel.setPublicKey(encryptHelper.publicKeyToBase64String(keyPair.getPublic()));
            respModel.setNonce(nonce);
            return respModel;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare public key for login.", e);
        }
    }

    /**
     * 登入第 2 步：解密密碼並驗證，成功後簽發 access token 與 refresh token
     */
    public RespLoginModel login(ReqLoginModel reqModel) {
        // 取出同時刪除：不論登入成功與否，這組 nonce／私鑰都只能使用一次
        LoginKeyEntity loginKeyEntity = redisLoginKeyRepository.takeByNonce(reqModel.getNonce())
                .orElseThrow(() -> new AppAuthenticationException("登入逾時，請重新登入"));

        if (!loginKeyEntity.getUsername().equals(reqModel.getUsername())) {
            throw new AppAuthenticationException(LOGIN_FAILED_MESSAGE);
        }

        String username = reqModel.getUsername();
        if (loginAttemptService.isLocked(username)) {
            // 鎖定期間不檢查密碼，回覆與密碼錯誤相同的訊息（不透露帳號是否存在、是否被鎖定）
            log.info("Login rejected: account is locked. username: {}", username);
            throw new AppAuthenticationException(LOGIN_FAILED_MESSAGE);
        }

        String rawPassword;
        try {
            rawPassword = decryptRawPassword(reqModel, loginKeyEntity);
        } catch (AppAuthenticationException e) {
            loginAttemptService.recordFailure(username);
            throw e;
        }

        AppUserDetails userDetails;
        try {
            userDetails = userHelper.findUserDetailsBySigninAccount(username);
        } catch (UsernameNotFoundException e) {
            log.info("Login failed: account not found. username: {}", username);
            loginAttemptService.recordFailure(username);
            throw new AppAuthenticationException(LOGIN_FAILED_MESSAGE);
        }

        if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            log.info("Login failed: wrong password. username: {}", username);
            loginAttemptService.recordFailure(username);
            throw new AppAuthenticationException(LOGIN_FAILED_MESSAGE);
        }

        loginAttemptService.reset(username);

        if (!userDetails.isEnabled()) {
            throw new AppAuthenticationException("此帳號已停用");
        }

        log.info("Login succeeded. username: {}", reqModel.getUsername());
        return issueTokens(userDetails);
    }

    /**
     * 用尚未過期、也尚未登出的 refresh token 換發新的 access token。
     * <br>
     * refresh token 本身不會換新（回應中原樣帶回），在有效期間內可以重複使用：
     * 前端同時有多個請求遇到 access token 過期、各自換發時，才不會互相讓對方的 refresh token 失效
     */
    public RespLoginModel refreshToken(String refreshToken) {
        Claims claims = parseRefreshToken(refreshToken);
        RefreshTokenEntity stored = redisRefreshTokenRepository.findByTokenId(claims.getId())
                .filter(entity -> entity.getUsername().equals(claims.getSubject()))
                .orElseThrow(() -> new AppAuthenticationException("請重新登入"));

        AppUserDetails userDetails;
        try {
            userDetails = userHelper.findUserDetailsBySigninAccount(stored.getUsername());
        } catch (UsernameNotFoundException e) {
            throw new AppAuthenticationException("請重新登入");
        }

        if (!userDetails.isEnabled()) {
            throw new AppAuthenticationException("此帳號已停用");
        }

        Date now = new Date();
        RespLoginModel respModel = buildResponse(userDetails);
        respModel.setAccessToken(jwtHelper.generateAccessToken(userDetails.getUsername(), now));
        respModel.setExpiredAt(jwtHelper.accessTokenExpiredAt(now));
        respModel.setRefreshToken(refreshToken);
        return respModel;
    }

    /**
     * 登出：作廢指定的 refresh token。token 無效或已作廢時不做任何事
     */
    public void logout(String refreshToken) {
        try {
            redisRefreshTokenRepository.deleteByTokenId(parseRefreshToken(refreshToken).getId());
        } catch (AppAuthenticationException e) {
            log.debug("Logout with an invalid refresh token.");
        }
    }

    private Claims parseRefreshToken(String refreshToken) {
        try {
            return jwtHelper.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AppAuthenticationException("請重新登入");
        }
    }

    private String decryptRawPassword(ReqLoginModel reqModel, LoginKeyEntity loginKeyEntity) {
        try {
            PrivateKey privateKey = encryptHelper.base64StringToPrivateKey(loginKeyEntity.getPrivateKeyBase64());
            SecretKey aesKey = encryptHelper.decryptAESKeyWithRSAPrivateKey(
                    encryptHelper.base64Decode(reqModel.getEncryption()), privateKey);
            byte[] ivBytes = encryptHelper.base64Decode(reqModel.getNonce());
            byte[] encryptedPasswordBytes = encryptHelper.base64Decode(reqModel.getPassword());
            return encryptHelper.toString(encryptHelper.decryptDataWithAES(aesKey, ivBytes, encryptedPasswordBytes));
        } catch (Exception e) {
            log.info("Login failed: cannot decrypt password. username: {}", reqModel.getUsername());
            throw new AppAuthenticationException(LOGIN_FAILED_MESSAGE, e);
        }
    }

    /**
     * 登入成功：簽發 access token 與新的 refresh token，refresh token 編號存到 Redis
     */
    private RespLoginModel issueTokens(AppUserDetails userDetails) {
        String username = userDetails.getUsername();
        Date now = new Date();
        String tokenId = jwtHelper.newTokenId();

        redisRefreshTokenRepository.save(new RefreshTokenEntity(tokenId, username));

        RespLoginModel respModel = buildResponse(userDetails);
        respModel.setAccessToken(jwtHelper.generateAccessToken(username, now));
        respModel.setExpiredAt(jwtHelper.accessTokenExpiredAt(now));
        respModel.setRefreshToken(jwtHelper.generateRefreshToken(username, tokenId, now));
        return respModel;
    }

    private RespLoginModel buildResponse(AppUserDetails userDetails) {
        RespLoginModel respModel = new RespLoginModel();
        respModel.setUserId(userDetails.getUser().getUserId());
        respModel.setUsername(userDetails.getUsername());
        respModel.setRoleNames(userDetails.getRoles());
        respModel.setPermissionNames(userDetails.getPermissions());
        respModel.setTenantId(userDetails.getTenantId());
        respModel.setTenantName(userDetails.getTenant() != null ? userDetails.getTenant().getTenantName() : null);
        respModel.setMustChangePassword(userDetails.isMustChangePassword());
        return respModel;
    }

    private String generateNonce() {
        byte[] nonceBytes = new byte[NONCE_LENGTH_IN_BYTES];
        secureRandom.nextBytes(nonceBytes);
        return encryptHelper.base64Encode(nonceBytes);
    }
}
