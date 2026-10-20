package com.example.starter.api.controller.tenant;

import com.example.starter.api.model.req.ReqCustomerModel;
import com.example.starter.business.common.PageModel;
import com.example.starter.business.customer.model.CustomerModel;
import com.example.starter.business.customer.service.CustomerService;
import com.example.starter.log.annotation.NoLogToParam;
import com.example.starter.user.model.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>CustomerController</code> 客戶（範例業務 API）。
 * <br>
 * 讀取：租戶帳號是自己的租戶；總管理員可以帶 {@code tenantId} 檢視某一個（不帶是全部租戶）。
 * 新增、修改、刪除：只有租戶帳號（總管理員只能檢視，資料庫的 Row-Level Security 也會拒絕）。
 * 查詢參數可能含客戶名稱，日誌不記錄（{@link NoLogToParam}）。
 */
@NoLogToParam
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final String TENANT_USER = "hasRole('TENANT_USER')";

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<PageModel<CustomerModel>> search(@AuthenticationPrincipal AppPrincipal principal,
                                                           @RequestParam(name = "keyword", required = false) String keyword,
                                                           @RequestParam(name = "tenantId", required = false) String tenantId,
                                                           @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive,
                                                           @RequestParam(name = "page", required = false) Integer page,
                                                           @RequestParam(name = "size", required = false) Integer size) {
        return ResponseEntity.ok(customerService.search(keyword, principal.sysAdmin() ? tenantId : null, includeInactive, page, size));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerModel> get(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(customerService.get(customerId));
    }

    @PreAuthorize(TENANT_USER)
    @PostMapping
    public ResponseEntity<CustomerModel> create(@AuthenticationPrincipal AppPrincipal principal, @RequestBody ReqCustomerModel reqModel) {
        return ResponseEntity.ok(customerService.create(reqModel.toInput(), principal.userId()));
    }

    @PreAuthorize(TENANT_USER)
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerModel> update(@AuthenticationPrincipal AppPrincipal principal,
                                                @PathVariable("customerId") String customerId,
                                                @RequestBody ReqCustomerModel reqModel) {
        return ResponseEntity.ok(customerService.update(customerId, reqModel.toInput(), principal.userId()));
    }

    /**
     * 刪除客戶：只限沒有任何訂單的客戶，有訂單回 409
     */
    @PreAuthorize(TENANT_USER)
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> delete(@PathVariable("customerId") String customerId) {
        customerService.delete(customerId);
        return ResponseEntity.noContent().build();
    }
}
