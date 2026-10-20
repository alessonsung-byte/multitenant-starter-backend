package com.example.starter.api.controller;

import com.example.starter.api.model.req.ReqLoginModel;
import com.example.starter.api.model.req.ReqRefreshTokenModel;
import com.example.starter.api.model.res.RespLoginModel;
import com.example.starter.api.model.res.RespPublicKeyModel;
import com.example.starter.api.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>PublicController</code> 不需要登入即可呼叫的 API：登入、換發 access token、登出。
 * 完整流程見 doc/HOW_TO_LOGIN.md
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final LoginService loginService;

    /**
     * 登入第 1 步：取得這次登入專用的公鑰與一次性標誌（nonce）
     */
    @GetMapping("/publickey/{username}")
    public ResponseEntity<RespPublicKeyModel> findPublicKey(@PathVariable("username") String username) {
        return ResponseEntity.ok(loginService.findPublicKey(username));
    }

    /**
     * 登入第 2 步：送出加密後的密碼，成功時回傳 access token 與 refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<RespLoginModel> login(@Valid @RequestBody ReqLoginModel reqModel) {
        return ResponseEntity.ok(loginService.login(reqModel));
    }

    /**
     * 用 refresh token 換發新的 access token
     */
    @PostMapping("/refreshToken")
    public ResponseEntity<RespLoginModel> refreshToken(@Valid @RequestBody ReqRefreshTokenModel reqModel) {
        return ResponseEntity.ok(loginService.refreshToken(reqModel.getRefreshToken()));
    }

    /**
     * 登出：作廢 refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody ReqRefreshTokenModel reqModel) {
        loginService.logout(reqModel.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
