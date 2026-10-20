package com.example.starter.business.report.service;

import com.example.starter.business.customer.entity.CustomerEntity;
import com.example.starter.business.customer.repository.CustomerRepository;
import com.example.starter.business.order.entity.OrderStatus;
import com.example.starter.business.report.model.ReportModels.CustomerRow;
import com.example.starter.business.report.model.ReportModels.MonthlyReport;
import com.example.starter.business.report.model.ReportModels.TenantReport;
import com.example.starter.business.report.model.ReportModels.TenantRow;
import com.example.starter.common.exception.InvalidArgumentException;
import com.example.starter.user.entity.TenantEntity;
import com.example.starter.user.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <code>ReportService</code> 月報表（範例業務模組）。
 * <br>
 * 統計一律交給資料庫（GROUP BY），不把訂單全部讀進記憶體。查詢 {@code SalesOrderEntity} 時 Hibernate 自動加上
 * 「租戶 = 目前租戶」，資料庫的 Row-Level Security 再擋一層，所以租戶帳號的報表只會算到自己的資料。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final CustomerRepository customerRepository;

    private final TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 一個月份每位客戶的訂單筆數與金額
     *
     * @param month    月份（yyyy-MM）
     * @param tenantId 總管理員檢視某一個租戶時使用（必填，避免把所有租戶混在一起）；租戶帳號傳 null
     * @param sysAdmin 是否為總管理員
     */
    @Transactional(readOnly = true)
    public MonthlyReport monthly(String month, String tenantId, boolean sysAdmin) {
        YearMonth yearMonth = parseMonth(month);
        if (sysAdmin && StringUtils.isBlank(tenantId)) {
            throw new InvalidArgumentException("請選擇租戶");
        }
        String jpql = "SELECT o.customerId, COUNT(o), SUM(o.subtotal) FROM SalesOrderEntity o "
                + "WHERE o.orderStatus = :status AND o.orderDate >= :from AND o.orderDate < :to"
                + (sysAdmin ? " AND o.tenantId = :tenantId" : "")
                + " GROUP BY o.customerId";
        TypedQuery<Object[]> query = monthQuery(jpql, yearMonth);
        if (sysAdmin) {
            query.setParameter("tenantId", tenantId);
        }
        List<Object[]> result = query.getResultList();

        Map<String, String> names = customerRepository.findAllById(result.stream().map(row -> (String) row[0]).toList()).stream()
                .collect(Collectors.toMap(CustomerEntity::getCustomerId, CustomerEntity::getCustomerName));
        List<CustomerRow> rows = result.stream()
                .map(row -> new CustomerRow((String) row[0], names.get((String) row[0]), ((Number) row[1]).longValue(), amount(row[2])))
                .sorted(Comparator.comparing(CustomerRow::amount).reversed().thenComparing(CustomerRow::customerId))
                .toList();
        return new MonthlyReport(yearMonth.toString(), rows, rows.stream().mapToLong(CustomerRow::orderCount).sum(),
                                 rows.stream().map(CustomerRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * 總管理員：一個月份每個租戶的訂單筆數與金額並列，加總全部租戶
     */
    @Transactional(readOnly = true)
    public TenantReport tenants(String month) {
        YearMonth yearMonth = parseMonth(month);
        List<Object[]> result = monthQuery("SELECT o.tenantId, COUNT(o), SUM(o.subtotal) FROM SalesOrderEntity o "
                                                   + "WHERE o.orderStatus = :status AND o.orderDate >= :from AND o.orderDate < :to "
                                                   + "GROUP BY o.tenantId", yearMonth).getResultList();
        Map<String, Object[]> byTenant = new HashMap<>();
        result.forEach(row -> byTenant.put((String) row[0], row));

        Map<String, TenantEntity> tenants = tenantRepository.findAllByOrderByCreateTimeAsc().stream()
                .collect(Collectors.toMap(TenantEntity::getTenantId, Function.identity(), (a, b) -> a, java.util.LinkedHashMap::new));
        List<TenantRow> rows = tenants.values().stream().map(tenant -> {
            Object[] row = byTenant.get(tenant.getTenantId());
            return new TenantRow(tenant.getTenantId(), tenant.getTenantName(), row == null ? 0L : ((Number) row[1]).longValue(),
                                 row == null ? BigDecimal.ZERO : amount(row[2]));
        }).toList();
        return new TenantReport(yearMonth.toString(), rows, rows.stream().mapToLong(TenantRow::orderCount).sum(),
                                rows.stream().map(TenantRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private TypedQuery<Object[]> monthQuery(String jpql, YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        return entityManager.createQuery(jpql, Object[].class)
                .setParameter("status", OrderStatus.NORMAL)
                .setParameter("from", from)
                .setParameter("to", from.plusMonths(1));
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(StringUtils.trimToEmpty(month));
        } catch (DateTimeParseException ex) {
            throw new InvalidArgumentException("月份格式必須是 yyyy-MM，例如 2026-09");
        }
    }

    private static BigDecimal amount(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }
}
