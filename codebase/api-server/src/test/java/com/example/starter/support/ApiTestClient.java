package com.example.starter.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>ApiTestClient</code> 整合測試呼叫 API 的工具，登入與修改密碼都照前端的方式加密
 * （RSA 公鑰加密隨機 AES 密鑰、AES-CBC 以 nonce 為 IV 加密密碼，見 doc/HOW_TO_LOGIN.md）。
 */
public class ApiTestClient {

    private final MockMvc mockMvc;

    private final ObjectMapper objectMapper;

    public ApiTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /**
     * 登入結果：HTTP 狀態碼與回應內容
     */
    public record LoginResult(int status, JsonNode body) {

        public String accessToken() {
            return body.get("accessToken").asText();
        }

        public String refreshToken() {
            return body.get("refreshToken").asText();
        }

        public String message() {
            return body.path("message").asText();
        }
    }

    /**
     * 呼叫結果：HTTP 狀態碼與回應內容（沒有內容時為空的 JSON）
     */
    public record Response(int status, JsonNode body) {
    }

    public LoginResult login(String username, String password) {
        JsonNode key = send(MockMvcRequestBuilders.get("/api/public/publickey/{username}", username), null).body();
        String nonce = key.get("nonce").asText();
        Encrypted encrypted = encrypt(key.get("publicKey").asText(), nonce, password);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", encrypted.values()[0]);
        body.put("nonce", nonce);
        body.put("encryption", encrypted.encryptedKey());
        Response response = send(MockMvcRequestBuilders.post("/api/public/login"), body);
        return new LoginResult(response.status(), response.body());
    }

    public String loginToken(String username, String password) {
        LoginResult result = login(username, password);
        assertThat(result.status()).as("login %s: %s", username, result.body()).isEqualTo(200);
        return result.accessToken();
    }

    /**
     * 修改密碼，回傳 HTTP 狀態碼（成功為 204）
     */
    public int changePassword(String accessToken, String username, String currentPassword, String newPassword) {
        return changePasswordResponse(accessToken, username, currentPassword, newPassword).status();
    }

    public Response changePasswordResponse(String accessToken, String username, String currentPassword, String newPassword) {
        JsonNode key = send(MockMvcRequestBuilders.get("/api/public/publickey/{username}", username), null).body();
        String nonce = key.get("nonce").asText();
        Encrypted encrypted = encrypt(key.get("publicKey").asText(), nonce, currentPassword, newPassword);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("nonce", nonce);
        body.put("encryption", encrypted.encryptedKey());
        body.put("currentPassword", encrypted.values()[0]);
        body.put("newPassword", encrypted.values()[1]);
        return send(withToken(MockMvcRequestBuilders.post("/api/me/password"), accessToken), body);
    }

    public Response refresh(String refreshToken) {
        return send(MockMvcRequestBuilders.post("/api/public/refreshToken"), Map.of("refreshToken", refreshToken));
    }

    public Response get(String accessToken, String url, Object... uriVariables) {
        return send(withToken(MockMvcRequestBuilders.get(url, uriVariables), accessToken), null);
    }

    public Response post(String accessToken, String url, Object body, Object... uriVariables) {
        return send(withToken(MockMvcRequestBuilders.post(url, uriVariables), accessToken), body);
    }

    public Response put(String accessToken, String url, Object body, Object... uriVariables) {
        return send(withToken(MockMvcRequestBuilders.put(url, uriVariables), accessToken), body);
    }

    public Response delete(String accessToken, String url, Object... uriVariables) {
        return send(withToken(MockMvcRequestBuilders.delete(url, uriVariables), accessToken), null);
    }

    /**
     * GET 並回傳原始內容（下載檔案用）
     *
     * @return [狀態碼, Content-Type, Content-Disposition, 內容]
     */
    public Download download(String accessToken, String url, Object... uriVariables) {
        try {
            MvcResult result = mockMvc.perform(withToken(MockMvcRequestBuilders.get(url, uriVariables), accessToken)).andReturn();
            return new Download(result.getResponse().getStatus(), result.getResponse().getContentType(),
                                result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION), result.getResponse().getContentAsByteArray());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record Download(int status, String contentType, String contentDisposition, byte[] content) {
    }

    /**
     * POST 並確認狀態碼，回傳回應內容
     */
    public JsonNode postJson(String accessToken, String url, Object body, int expectedStatus) {
        Response response = post(accessToken, url, body);
        assertThat(response.status()).as("POST %s: %s", url, response.body()).isEqualTo(expectedStatus);
        return response.body();
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder, String accessToken) {
        return accessToken == null ? builder : builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private Response send(MockHttpServletRequestBuilder builder, Object body) {
        try {
            if (body != null) {
                builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
            }
            MvcResult result = mockMvc.perform(builder).andReturn();
            String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            JsonNode json = content.isEmpty() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
            return new Response(result.getResponse().getStatus(), json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record Encrypted(String encryptedKey, String[] values) {
    }

    /**
     * 與前端 lib/login-cipher.ts 相同的加密方式：同一把隨機 AES 密鑰加密所有值，再以 RSA 公鑰加密這把 AES 密鑰
     */
    private static Encrypted encrypt(String publicKeyBase64, String nonceBase64, String... plainValues) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey aesKey = keyGenerator.generateKey();

            String[] values = new String[plainValues.length];
            for (int i = 0; i < plainValues.length; i++) {
                Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
                aes.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(Base64.getDecoder().decode(nonceBase64)));
                values[i] = Base64.getEncoder().encodeToString(aes.doFinal(plainValues[i].getBytes(StandardCharsets.UTF_8)));
            }

            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            return new Encrypted(Base64.getEncoder().encodeToString(rsa.doFinal(aesKey.getEncoded())), values);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
