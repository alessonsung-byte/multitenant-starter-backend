package com.example.starter.api.controller.admin;

import com.example.starter.api.model.req.ReqTenantModel;
import com.example.starter.user.model.AppPrincipal;
import com.example.starter.user.model.TenantModel;
import com.example.starter.user.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <code>AdminTenantController</code> 總管理員管理租戶：列表、新增、改名、啟用／停用
 */
@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class AdminTenantController {

    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<List<TenantModel>> findAll() {
        return ResponseEntity.ok(tenantService.findAll());
    }

    @PostMapping
    public ResponseEntity<TenantModel> create(@AuthenticationPrincipal AppPrincipal principal,
                                              @Valid @RequestBody ReqTenantModel reqModel) {
        return ResponseEntity.ok(tenantService.create(reqModel.getTenantName(), principal.userId()));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantModel> rename(@AuthenticationPrincipal AppPrincipal principal,
                                              @PathVariable("tenantId") String tenantId,
                                              @Valid @RequestBody ReqTenantModel reqModel) {
        return ResponseEntity.ok(tenantService.rename(tenantId, reqModel.getTenantName(), principal.userId()));
    }

    @PostMapping("/{tenantId}/enable")
    public ResponseEntity<TenantModel> enable(@AuthenticationPrincipal AppPrincipal principal,
                                              @PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(tenantService.setEnabled(tenantId, true, principal.userId()));
    }

    /**
     * 停用租戶：該租戶所有帳號的下一次請求就無法使用
     */
    @PostMapping("/{tenantId}/disable")
    public ResponseEntity<TenantModel> disable(@AuthenticationPrincipal AppPrincipal principal,
                                               @PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(tenantService.setEnabled(tenantId, false, principal.userId()));
    }
}
