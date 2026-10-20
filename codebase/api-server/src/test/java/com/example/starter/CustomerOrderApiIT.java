package com.example.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.starter.support.ApiTestClient;
import com.example.starter.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>CustomerOrderApiIT</code> 範例業務模組的規則：客戶的新增／修改／刪除、訂單的金額計算與作廢、查詢合計、月報表
 */
class CustomerOrderApiIT extends IntegrationTestBase {

    private ApiTestClient api;

    private TenantFixture tenant;

    @BeforeEach
    void setUp() {
        api = api();
        tenant = newTenantWithAccount("Biz");
    }

    @Test
    void customerNameIsRequiredAndCodeIsUniqueWithinTheTenant() {
        assertThat(api.post(tenant.token(), "/api/customers", Map.of("customerName", "  ")).status()).isEqualTo(400);

        api.postJson(tenant.token(), "/api/customers", Map.of("customerName", "甲", "customerCode", "X-1"), 200);
        assertThat(api.post(tenant.token(), "/api/customers", Map.of("customerName", "乙", "customerCode", "X-1")).status()).isEqualTo(409);

        // 其他租戶可以使用相同代碼
        TenantFixture other = newTenantWithAccount("BizOther");
        assertThat(api.post(other.token(), "/api/customers", Map.of("customerName", "丙", "customerCode", "X-1")).status()).isEqualTo(200);
    }

    @Test
    void inactiveCustomersAreHiddenByDefaultAndCannotReceiveNewOrders() {
        String customerId = createCustomer(tenant.token(), "停止往來");
        api.put(tenant.token(), "/api/customers/{id}", Map.of("customerName", "停止往來", "active", false), customerId);

        assertThat(api.get(tenant.token(), "/api/customers?size=100").body().get("items").findValuesAsText("customerId"))
                .doesNotContain(customerId);
        assertThat(api.get(tenant.token(), "/api/customers?size=100&includeInactive=true").body().get("items").findValuesAsText("customerId"))
                .contains(customerId);
        assertThat(api.post(tenant.token(), "/api/orders", Map.of("customerId", customerId, "orderDate", "2026-09-10", "itemName", "x",
                                                                  "unitPrice", 1, "quantity", 1)).status()).isEqualTo(400);
    }

    @Test
    void customerWithOrdersCannotBeDeleted() {
        String withOrder = createCustomer(tenant.token(), "有訂單");
        createOrder(tenant.token(), withOrder);
        String withoutOrder = createCustomer(tenant.token(), "沒有訂單");

        assertThat(api.delete(tenant.token(), "/api/customers/{id}", withOrder).status()).isEqualTo(409);
        assertThat(api.delete(tenant.token(), "/api/customers/{id}", withoutOrder).status()).isEqualTo(204);
        assertThat(api.get(tenant.token(), "/api/customers/{id}", withoutOrder).status()).isEqualTo(404);
    }

    @Test
    void subtotalIsCalculatedByTheServerAndInputIsValidated() {
        String customerId = createCustomer(tenant.token(), "金額");
        JsonNode order = api.postJson(tenant.token(), "/api/orders", Map.of("customerId", customerId, "orderDate", "2026-09-10",
                                                                             "itemName", "品項", "unitPrice", 12.5, "quantity", 3,
                                                                             "subtotal", 1), 200);
        assertThat(order.get("subtotal").decimalValue()).isEqualByComparingTo("37.5");

        assertThat(post(customerId, "2999-01-01", "1", 1)).as("未來日期").isEqualTo(400);
        assertThat(post(customerId, "2026-09-10", "-1", 1)).as("負數單價").isEqualTo(400);
        assertThat(post(customerId, "2026-09-10", "1.001", 1)).as("三位小數").isEqualTo(400);
        assertThat(post(customerId, "2026-09-10", "1", 0)).as("數量 0").isEqualTo(400);
        assertThat(post(customerId, "2026-09-10", "1", 1000)).as("數量超過上限").isEqualTo(400);
    }

    @Test
    void voidedOrdersAreExcludedFromTotalsAndCannotBeVoidedTwice() {
        String customerId = createCustomer(tenant.token(), "作廢");
        createOrder(tenant.token(), customerId, "2026-08-01", "100", 1);
        String voided = createOrder(tenant.token(), customerId, "2026-08-02", "50", 2);

        assertThat(api.post(tenant.token(), "/api/orders/{id}/void", Map.of("reason", " "), voided).status()).isEqualTo(400);
        assertThat(api.post(tenant.token(), "/api/orders/{id}/void", Map.of("reason", "重複登打"), voided).status()).isEqualTo(200);
        assertThat(api.post(tenant.token(), "/api/orders/{id}/void", Map.of("reason", "再一次"), voided).status()).isEqualTo(409);

        JsonNode page = api.get(tenant.token(), "/api/orders?customerId={id}&includeVoided=true", customerId).body();
        assertThat(page.get("total").asLong()).isEqualTo(2);
        assertThat(page.get("validCount").asLong()).isEqualTo(1);
        assertThat(page.get("validAmount").decimalValue()).isEqualByComparingTo("100");
        assertThat(api.get(tenant.token(), "/api/orders?customerId={id}", customerId).body().get("total").asLong()).isEqualTo(1);

        JsonNode report = api.get(tenant.token(), "/api/reports/monthly?month=2026-08").body();
        assertThat(report.get("totalCount").asLong()).isEqualTo(1);
        assertThat(report.get("totalAmount").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void monthlyReportGroupsByCustomerAndAdminReportListsEveryTenant() {
        String big = createCustomer(tenant.token(), "大客戶");
        String small = createCustomer(tenant.token(), "小客戶");
        createOrder(tenant.token(), big, "2025-03-01", "1000", 2);
        createOrder(tenant.token(), big, "2025-03-31", "500", 1);
        createOrder(tenant.token(), small, "2025-03-15", "10", 1);
        createOrder(tenant.token(), small, "2025-04-01", "999", 1);

        JsonNode report = api.get(tenant.token(), "/api/reports/monthly?month=2025-03").body();
        assertThat(report.get("rows").findValuesAsText("customerName")).containsExactly("大客戶", "小客戶");
        assertThat(report.get("rows").get(0).get("orderCount").asLong()).isEqualTo(2);
        assertThat(report.get("rows").get(0).get("amount").decimalValue()).isEqualByComparingTo("2500");
        assertThat(report.get("totalAmount").decimalValue()).isEqualByComparingTo("2510");
        assertThat(api.get(tenant.token(), "/api/reports/monthly?month=2025-3x").status()).isEqualTo(400);

        String adminToken = adminToken();
        assertThat(api.get(adminToken, "/api/reports/monthly?month=2025-03").status()).as("總管理員必須指定租戶").isEqualTo(400);
        JsonNode tenants = api.get(adminToken, "/api/admin/reports/tenants?month=2025-03").body();
        JsonNode row = null;
        for (JsonNode candidate : tenants.get("rows")) {
            if (candidate.get("tenantId").asText().equals(tenant.tenantId())) {
                row = candidate;
            }
        }
        assertThat(row).isNotNull();
        assertThat(row.get("amount").decimalValue()).isEqualByComparingTo("2510");
        assertThat(row.get("tenantName").asText()).isEqualTo(tenant.tenantName());
    }

    private int post(String customerId, String orderDate, String unitPrice, int quantity) {
        return api.post(tenant.token(), "/api/orders", Map.of("customerId", customerId, "orderDate", orderDate, "itemName", "x",
                                                              "unitPrice", new java.math.BigDecimal(unitPrice), "quantity", quantity)).status();
    }
}
