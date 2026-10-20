package com.example.starter.api.controller;

import com.example.starter.api.model.req.ReqChangePasswordModel;
import com.example.starter.user.model.CurrentUserModel;
import com.example.starter.user.model.AppPrincipal;
import com.example.starter.user.model.AppUserDetails;
import com.example.starter.user.service.PasswordChangeService;
import com.example.starter.user.service.UserHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>MeController</code> 目前登入者自己的資料與修改密碼（所有登入者都可以使用）
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserHelper userHelper;

    private final PasswordChangeService passwordChangeService;

    /**
     * 目前登入者：帳號、角色、租戶 id 與名稱、是否需要改密碼
     */
    @GetMapping
    public ResponseEntity<CurrentUserModel> findMe(@AuthenticationPrincipal AppPrincipal principal) {
        AppUserDetails userDetails = userHelper.findUserDetailsBySigninAccount(principal.username());
        return ResponseEntity.ok(CurrentUserModel.from(userDetails));
    }

    /**
     * 修改密碼（密碼以 RSA＋AES 加密傳送，見 {@link ReqChangePasswordModel}）
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AppPrincipal principal,
                                               @Valid @RequestBody ReqChangePasswordModel reqModel) {
        passwordChangeService.changePassword(principal.username(), reqModel.getNonce(), reqModel.getEncryption(),
                                             reqModel.getCurrentPassword(), reqModel.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
