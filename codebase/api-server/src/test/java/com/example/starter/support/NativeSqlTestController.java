package com.example.starter.support;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>NativeSqlTestController</code> 只在測試中存在的 API：以手寫 SQL 計算資料表筆數，
 * 證明經過「登入 → 目前租戶 → 交易設定租戶」整條路徑後，手寫 SQL 也只算得到自己租戶的資料。
 * 正式程式沒有這支 API。
 */
@RestController
@RequiredArgsConstructor
public class NativeSqlTestController {

    private final NativeSqlCounter nativeSqlCounter;

    @GetMapping("/api/test/native-count")
    public ResponseEntity<Long> count(@RequestParam("table") String table) {
        return ResponseEntity.ok(nativeSqlCounter.count(table));
    }
}
