package com.example.starter.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * <code>EncryptHelper</code> 登入流程用的加解密工具（流程見 doc/HOW_TO_LOGIN.md）：
 * 產生一次性 RSA 金鑰對，並解開前端以 RSA＋AES 加密送來的密碼
 */
@Service
public class EncryptHelper {

    @Value("${encrypt.rsa.keysize}")
    private int rsaKeySize;

    /**
     * 產生 RSA 公鑰與私鑰
     */
    public KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(rsaKeySize);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 使用 RSA 私鑰解密 AES 密鑰
     */
    public SecretKey decryptAESKeyWithRSAPrivateKey(byte[] encryptedAESKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedAESKey);
        return new SecretKeySpec(decryptedKeyBytes, "AES");
    }

    /**
     * 使用 AES（CBC、PKCS5Padding）與 IV 解密資料
     */
    public byte[] decryptDataWithAES(SecretKey aesKey, byte[] iv, byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        return cipher.doFinal(encryptedData);
    }

    public String publicKeyToBase64String(PublicKey publicKey) {
        return base64Encode(publicKey.getEncoded());
    }

    public String privateKeyToBase64String(PrivateKey privateKey) {
        return base64Encode(privateKey.getEncoded());
    }

    public PrivateKey base64StringToPrivateKey(String base64PrivateKeyString) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(base64Decode(base64PrivateKeyString));
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public String base64Encode(byte[] rawBytes) {
        return Base64.getEncoder().encodeToString(rawBytes);
    }

    public byte[] base64Decode(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
