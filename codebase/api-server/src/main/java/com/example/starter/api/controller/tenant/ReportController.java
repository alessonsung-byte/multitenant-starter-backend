package com.example.starter.api.controller.tenant;

import com.example.starter.business.report.model.ReportModels.MonthlyReport;
import com.example.starter.business.report.service.ReportService;
import com.example.starter.user.model.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>ReportController</code> 租戶的月報表。總管理員必須帶 {@code tenantId} 指定要看哪一個租戶
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 這個月每位客戶的訂單筆數與金額
     *
     * @param month yyyy-MM
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReport> monthly(@AuthenticationPrincipal AppPrincipal principal,
                                                 @RequestParam("month") String month,
                                                 @RequestParam(name = "tenantId", required = false) String tenantId) {
        return ResponseEntity.ok(reportService.monthly(month, principal.sysAdmin() ? tenantId : null, principal.sysAdmin()));
    }
}
