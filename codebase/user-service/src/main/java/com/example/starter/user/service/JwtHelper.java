package com.example.starter.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * <code>JwtHelper</code> 負責簽發與解析 JWT（HMAC-SHA256 固定金鑰簽章）。
 * <br>
 * 簽發兩種 token，以自訂欄位 {@value #TOKEN_TYPE_CLAIM} 區分，避免互相冒用：
 * <ul>
 *     <li>access token：每次呼叫 API 時放在 HTTP header {@code Authorization: Bearer ...}</li>
 *     <li>refresh token：只能用來換發新的 access token，另帶一個唯一編號（jti），對應 Redis 中的紀錄</li>
 * </ul>
 */
@Component
public class JwtHelper {

    public static final String TOKEN_TYPE_CLAIM = "typ";

    public static final String ACCESS_TOKEN_TYPE = "access";

    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final Key signingKey;

    private final long accessTokenExpiredInMillis;

    private final long refreshTokenExpiredInMillis;

    public JwtHelper(@Value("${user.jwt.token.signing.key}") String base64SigningKey,
                     @Value("${user.jwt.token.expired.millis}") long accessTokenExpiredInMillis,
                     @Value("${user.jwt.refresh.token.expired.millis}") long refreshTokenExpiredInMillis) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64SigningKey));
        this.accessTokenExpiredInMillis = accessTokenExpiredInMillis;
        this.refreshTokenExpiredInMillis = refreshTokenExpiredInMillis;
    }

    /**
     * 簽發 access token
     *
     * @param username 登入帳號
     * @param issuedAt 簽發時間
     */
    public String generateAccessToken(String username, Date issuedAt) {
        return Jwts.builder()
                .setSubject(username)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setIssuedAt(issuedAt)
                .setExpiration(new Date(issuedAt.getTime() + accessTokenExpiredInMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 簽發 refresh token
     *
     * @param username 登入帳號
     * @param tokenId  這個 refresh token 的唯一編號，同時存到 Redis
     * @param issuedAt 簽發時間
     */
    public String generateRefreshToken(String username, String tokenId, Date issuedAt) {
        return Jwts.builder()
                .setSubject(username)
                .setId(tokenId)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .setIssuedAt(issuedAt)
                .setExpiration(new Date(issuedAt.getTime() + refreshTokenExpiredInMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 產生新的 refresh token 編號
     */
    public String newTokenId() {
        return UUID.randomUUID().toString();
    }

    /**
     * access token 的逾期時間（毫秒）
     */
    public long accessTokenExpiredAt(Date issuedAt) {
        return issuedAt.getTime() + accessTokenExpiredInMillis;
    }

    /**
     * 驗證 access token 的簽章、有效期間與類型，通過後回傳內容
     *
     * @throws JwtException 簽章錯誤、已過期、格式錯誤或類型不是 access token 時丟出
     */
    public Claims parseAccessToken(String token) {
        return parse(token, ACCESS_TOKEN_TYPE);
    }

    /**
     * 驗證 refresh token 的簽章、有效期間與類型，通過後回傳內容
     *
     * @throws JwtException 簽章錯誤、已過期、格式錯誤或類型不是 refresh token 時丟出
     */
    public Claims parseRefreshToken(String token) {
        return parse(token, REFRESH_TOKEN_TYPE);
    }

    private Claims parse(String token, String expectedType) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
        if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new JwtException("Unexpected token type.");
        }
        return claims;
    }
}
