package com.example.starter.api.controller.tenant;

import com.example.starter.api.model.req.ReqOrderModel;
import com.example.starter.api.model.req.ReqVoidOrderModel;
import com.example.starter.business.order.model.OrderModel;
import com.example.starter.business.order.model.OrderPageModel;
import com.example.starter.business.order.service.OrderService;
import com.example.starter.log.annotation.NoLogToParam;
import com.example.starter.user.model.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/**
 * <code>OrderController</code> 訂單（範例業務 API）。
 * <br>
 * 查詢：租戶帳號是自己的租戶；總管理員可以帶 {@code tenantId}。新增、作廢：只有租戶帳號。
 * 訂單沒有修改的 API（只能作廢後重新登打）。
 */
@NoLogToParam
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String TENANT_USER = "hasRole('TENANT_USER')";

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<OrderPageModel> search(@AuthenticationPrincipal AppPrincipal principal,
                                                 @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(name = "customerId", required = false) String customerId,
                                                 @RequestParam(name = "includeVoided", defaultValue = "false") boolean includeVoided,
                                                 @RequestParam(name = "tenantId", required = false) String tenantId,
                                                 @RequestParam(name = "page", required = false) Integer page,
                                                 @RequestParam(name = "size", required = false) Integer size) {
        return ResponseEntity.ok(orderService.search(from, to, customerId, includeVoided, principal.sysAdmin() ? tenantId : null, page, size));
    }

    @PreAuthorize(TENANT_USER)
    @PostMapping
    public ResponseEntity<OrderModel> create(@AuthenticationPrincipal AppPrincipal principal, @RequestBody ReqOrderModel reqModel) {
        return ResponseEntity.ok(orderService.create(reqModel.toInput(), principal.userId()));
    }

    /**
     * 作廢（必填原因）
     */
    @PreAuthorize(TENANT_USER)
    @PostMapping("/{orderId}/void")
    public ResponseEntity<OrderModel> voidOrder(@AuthenticationPrincipal AppPrincipal principal,
                                                @PathVariable("orderId") String orderId,
                                                @RequestBody(required = false) ReqVoidOrderModel reqModel) {
        return ResponseEntity.ok(orderService.voidOrder(orderId, reqModel == null ? null : reqModel.getReason(), principal.userId()));
    }
}
