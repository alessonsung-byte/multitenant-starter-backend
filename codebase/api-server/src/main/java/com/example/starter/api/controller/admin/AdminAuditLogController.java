package com.example.starter.api.controller.admin;

import com.example.starter.business.audit.model.AuditLogModel;
import com.example.starter.business.audit.service.AuditService;
import com.example.starter.business.common.PageModel;
import com.example.starter.log.annotation.NoLogToParam;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * <code>AdminAuditLogController</code> 操作紀錄查詢
 */
@NoLogToParam
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<PageModel<AuditLogModel>> search(@RequestParam(name = "tenantId", required = false) String tenantId,
                                                           @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                           @RequestParam(name = "page", required = false) Integer page,
                                                           @RequestParam(name = "size", required = false) Integer size) {
        return ResponseEntity.ok(auditService.search(tenantId, from, to, page, size));
    }
}
