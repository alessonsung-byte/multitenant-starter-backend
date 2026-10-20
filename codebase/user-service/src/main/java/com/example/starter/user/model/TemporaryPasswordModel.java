package com.example.starter.user.model;

/**
 * <code>TemporaryPasswordModel</code> 新增帳號、重設密碼的回應：帳號資料＋臨時密碼。
 * <br>
 * 臨時密碼只在這個回應出現一次，資料庫只存雜湊值，之後無法再查詢。
 *
 * @param account           帳號資料
 * @param temporaryPassword 臨時密碼（明碼，只顯示這一次）
 */
public record TemporaryPasswordModel(AccountModel account, String temporaryPassword) {
}
