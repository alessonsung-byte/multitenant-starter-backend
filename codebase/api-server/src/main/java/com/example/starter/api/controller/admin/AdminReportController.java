package com.example.starter.api.controller.admin;

import com.example.starter.business.report.model.ReportModels.TenantReport;
import com.example.starter.business.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>AdminReportController</code> 跨租戶整合報表（只有總管理員）
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * 各租戶這個月的訂單筆數、金額並列，加總全部租戶
     *
     * @param month yyyy-MM
     */
    @GetMapping("/tenants")
    public ResponseEntity<TenantReport> tenants(@RequestParam("month") String month) {
        return ResponseEntity.ok(reportService.tenants(month));
    }
}
