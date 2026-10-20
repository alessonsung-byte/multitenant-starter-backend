package com.example.starter.api.controller.admin;

import com.example.starter.api.model.req.ReqCreateAccountModel;
import com.example.starter.user.model.AccountModel;
import com.example.starter.user.model.TemporaryPasswordModel;
import com.example.starter.user.model.AppPrincipal;
import com.example.starter.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <code>AdminAccountController</code> 總管理員管理帳號：列表、新增、啟用／停用、重設臨時密碼。
 * <br>
 * 新增與重設密碼的回應中含有臨時密碼（只出現這一次），所以回應不寫入日誌，前端也不可保存。
 */
@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountService accountService;

    /**
     * 帳號列表；帶 tenantId 時只列出該租戶的帳號
     */
    @GetMapping
    public ResponseEntity<List<AccountModel>> findAll(@RequestParam(name = "tenantId", required = false) String tenantId) {
        return ResponseEntity.ok(accountService.findAll(tenantId));
    }

    /**
     * 新增帳號，回傳一次性臨時密碼
     */
    @PostMapping
    public ResponseEntity<TemporaryPasswordModel> create(@AuthenticationPrincipal AppPrincipal principal,
                                                         @Valid @RequestBody ReqCreateAccountModel reqModel) {
        return ResponseEntity.ok(accountService.create(reqModel.getAccount(), reqModel.getEmail(), reqModel.getRoleName(),
                                                       reqModel.getTenantId(), principal.userId()));
    }

    @PostMapping("/{userId}/enable")
    public ResponseEntity<AccountModel> enable(@AuthenticationPrincipal AppPrincipal principal,
                                               @PathVariable("userId") String userId) {
        return ResponseEntity.ok(accountService.setEnabled(userId, true, principal.userId()));
    }

    @PostMapping("/{userId}/disable")
    public ResponseEntity<AccountModel> disable(@AuthenticationPrincipal AppPrincipal principal,
                                                @PathVariable("userId") String userId) {
        return ResponseEntity.ok(accountService.setEnabled(userId, false, principal.userId()));
    }

    /**
     * 重設為新的臨時密碼（回傳一次）
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<TemporaryPasswordModel> resetPassword(@AuthenticationPrincipal AppPrincipal principal,
                                                                @PathVariable("userId") String userId) {
        return ResponseEntity.ok(accountService.resetPassword(userId, principal.userId()));
    }
}
