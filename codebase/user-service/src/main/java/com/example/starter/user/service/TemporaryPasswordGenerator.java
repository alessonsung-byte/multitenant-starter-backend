package com.example.starter.user.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * <code>TemporaryPasswordGenerator</code> 產生總管理員新增帳號、重設密碼時的臨時密碼。
 * <br>
 * 12 碼隨機英數字，排除容易看錯的字元（0／O、1／l／I），方便總管理員口頭或手寫轉交。
 * 臨時密碼只在畫面顯示一次，使用者第一次登入就必須改掉。
 */
@Component
public class TemporaryPasswordGenerator {

    public static final int LENGTH = 12;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder builder = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return builder.toString();
    }
}
