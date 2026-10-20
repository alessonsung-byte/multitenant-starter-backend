package com.example.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.starter.support.ApiTestClient;
import com.example.starter.support.ApiTestClient.Response;
import com.example.starter.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>TenantIsolationIT</code> 多租戶隔離：兩個租戶各自的帳號，透過每一支 API 都拿不到對方的資料。
 * <br>
 * 驗證整條路徑：登入 → 目前租戶（TenantContext）→ Hibernate 自動加上的租戶條件 → 資料庫 Row-Level Security。
 * 之後每新增一張屬於租戶的資料表或 API，都要在這裡加上對應的測試。
 */
class TenantIsolationIT extends IntegrationTestBase {

    private static final String CUSTOMERS = "/api/customers";

    private static final String ORDERS = "/api/orders";

    private ApiTestClient api;

    private TenantFixture tenantA;

    private TenantFixture tenantB;

    private String customerOfA;

    private String customerOfB;

    @BeforeEach
    void setUp() {
        api = api();
        tenantA = newTenantWithAccount("TenantA");
        tenantB = newTenantWithAccount("TenantB");
        customerOfA = createCustomer(tenantA.token(), "A 的客戶");
        customerOfB = createCustomer(tenantB.token(), "B 的客戶");
    }

    @Test
    void newDataIsAssignedToTheLoggedInTenantEvenIfAnotherTenantIdIsSent() {
        JsonNode created = api.postJson(tenantA.token(), CUSTOMERS, Map.of("customerName", "新客戶", "tenantId", tenantB.tenantId()), 200);

        assertThat(created.get("tenantId").asText()).isEqualTo(tenantA.tenantId());
        assertThat(ownerJdbc().queryForObject("SELECT tenant_id FROM mt_customer WHERE customer_id = ?", String.class,
                                              created.get("customerId").asText())).isEqualTo(tenantA.tenantId());
    }

    @Test
    void listOnlyContainsOwnTenantData() {
        assertThat(customerIds(api.get(tenantA.token(), CUSTOMERS + "?size=100"))).contains(customerOfA).doesNotContain(customerOfB);
        assertThat(customerIds(api.get(tenantB.token(), CUSTOMERS + "?size=100"))).contains(customerOfB).doesNotContain(customerOfA);
        // 租戶帳號帶別人的 tenantId 參數：被忽略，仍然只看到自己的
        assertThat(api.get(tenantA.token(), CUSTOMERS + "?size=100&tenantId=" + tenantB.tenantId()).body().get("items")
                           .findValuesAsText("tenantId")).containsOnly(tenantA.tenantId());
        // 以名稱搜尋其他租戶的客戶：找不到
        assertThat(api.get(tenantA.token(), CUSTOMERS + "?keyword={keyword}", "B 的客戶").body().get("total").asLong()).isZero();
    }

    @Test
    void handwrittenSqlOnlyCountsOwnTenantData() {
        createOrder(tenantA.token(), customerOfA);
        createOrder(tenantB.token(), customerOfB);
        for (String table : List.of("mt_customer", "mt_sales_order")) {
            long countInDatabaseForA = ownerJdbc().queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id = ?", Long.class,
                                                                  tenantA.tenantId());

            Response response = api.get(tenantA.token(), "/api/test/native-count?table=" + table);

            assertThat(response.status()).isEqualTo(200);
            assertThat(response.body().asLong()).as(table).isEqualTo(countInDatabaseForA);
        }
    }

    @Test
    void readingUpdatingOrDeletingOtherTenantDataReturnsNotFoundAndChangesNothing() {
        assertThat(api.get(tenantA.token(), CUSTOMERS + "/{id}", customerOfB).status()).isEqualTo(404);
        assertThat(api.put(tenantA.token(), CUSTOMERS + "/{id}", Map.of("customerName", "被 A 改掉"), customerOfB).status()).isEqualTo(404);
        assertThat(api.delete(tenantA.token(), CUSTOMERS + "/{id}", customerOfB).status()).isEqualTo(404);
        assertThat(api.get(tenantA.token(), CUSTOMERS + "/{id}", customerOfA).status()).isEqualTo(200);

        assertThat(ownerJdbc().queryForObject("SELECT customer_name FROM mt_customer WHERE customer_id = ?", String.class, customerOfB))
                .isEqualTo("B 的客戶");
    }

    @Test
    void ordersAreIsolatedAndCannotReferenceAnotherTenantsCustomer() {
        String orderOfB = createOrder(tenantB.token(), customerOfB);

        assertThat(api.post(tenantA.token(), ORDERS, Map.of("customerId", customerOfB, "orderDate", "2026-09-10", "itemName", "x",
                                                            "unitPrice", 1, "quantity", 1)).status()).isEqualTo(404);
        assertThat(api.post(tenantA.token(), ORDERS + "/{id}/void", Map.of("reason", "A 作廢 B 的訂單"), orderOfB).status()).isEqualTo(404);
        assertThat(api.get(tenantA.token(), ORDERS + "?includeVoided=true&size=100").body().get("items").findValuesAsText("orderId"))
                .doesNotContain(orderOfB);
        assertThat(api.get(tenantA.token(), ORDERS + "?customerId={id}", customerOfB).body().get("total").asLong()).isZero();

        assertThat(ownerJdbc().queryForObject("SELECT order_status FROM mt_sales_order WHERE order_id = ?", String.class, orderOfB))
                .isEqualTo("NORMAL");
    }

    @Test
    void reportsOnlyCountOwnTenantData() {
        createOrder(tenantA.token(), customerOfA, "2024-01-05", "300", 1);
        createOrder(tenantB.token(), customerOfB, "2024-01-06", "9999", 1);

        JsonNode reportOfA = api.get(tenantA.token(), "/api/reports/monthly?month=2024-01").body();
        assertThat(reportOfA.get("totalAmount").decimalValue()).isEqualByComparingTo("300");
        assertThat(reportOfA.get("rows").findValuesAsText("customerId")).containsExactly(customerOfA);
        // 租戶帳號帶 tenantId 也只能看到自己的
        assertThat(api.get(tenantA.token(), "/api/reports/monthly?month=2024-01&tenantId=" + tenantB.tenantId()).body()
                           .get("totalAmount").decimalValue()).isEqualByComparingTo("300");
    }

    @Test
    void sysAdminCanReadAllTenantsButCannotWriteBusinessData() {
        String adminToken = adminToken();
        String orderOfA = createOrder(tenantA.token(), customerOfA);

        assertThat(customerIds(api.get(adminToken, CUSTOMERS + "?size=100&tenantId=" + tenantA.tenantId())))
                .contains(customerOfA).doesNotContain(customerOfB);
        assertThat(api.get(adminToken, CUSTOMERS + "/{id}", customerOfB).status()).isEqualTo(200);
        assertThat(api.get(adminToken, "/api/reports/monthly?month=2026-09&tenantId=" + tenantA.tenantId()).status()).isEqualTo(200);

        assertThat(api.post(adminToken, CUSTOMERS, Map.of("customerName", "總管理員新增")).status()).isEqualTo(403);
        assertThat(api.put(adminToken, CUSTOMERS + "/{id}", Map.of("customerName", "總管理員修改"), customerOfA).status()).isEqualTo(403);
        assertThat(api.delete(adminToken, CUSTOMERS + "/{id}", customerOfB).status()).isEqualTo(403);
        assertThat(api.post(adminToken, ORDERS, Map.of("customerId", customerOfA, "orderDate", "2026-09-10", "itemName", "x",
                                                       "unitPrice", 1, "quantity", 1)).status()).isEqualTo(403);
        assertThat(api.post(adminToken, ORDERS + "/{id}/void", Map.of("reason", "x"), orderOfA).status()).isEqualTo(403);

        assertThat(ownerJdbc().queryForObject("SELECT count(*) FROM mt_customer WHERE customer_name = '總管理員新增'", Long.class)).isZero();
    }

    @Test
    void meReturnsOnlyOwnTenant() {
        JsonNode meOfA = api.get(tenantA.token(), "/api/me").body();

        assertThat(meOfA.get("tenantId").asText()).isEqualTo(tenantA.tenantId());
        assertThat(meOfA.get("tenantName").asText()).isEqualTo(tenantA.tenantName());
        assertThat(meOfA.get("roleName").asText()).isEqualTo("TENANT_USER");
        assertThat(api.get(tenantB.token(), "/api/me").body().get("tenantId").asText()).isEqualTo(tenantB.tenantId());
    }

    @Test
    void tenantAccountCannotUseAnyAdminApi() {
        String token = tenantA.token();
        String someUserId = api.get(tenantB.token(), "/api/me").body().get("userId").asText();

        assertThat(api.get(token, "/api/admin/tenants").status()).isEqualTo(403);
        assertThat(api.post(token, "/api/admin/tenants", Map.of("tenantName", uniqueName("x"))).status()).isEqualTo(403);
        assertThat(api.put(token, "/api/admin/tenants/{id}", Map.of("tenantName", "改名"), tenantB.tenantId()).status()).isEqualTo(403);
        assertThat(api.post(token, "/api/admin/tenants/{id}/disable", null, tenantB.tenantId()).status()).isEqualTo(403);
        assertThat(api.get(token, "/api/admin/accounts").status()).isEqualTo(403);
        assertThat(api.post(token, "/api/admin/accounts",
                            Map.of("account", uniqueName("x"), "roleName", "TENANT_USER", "tenantId", tenantA.tenantId())).status()).isEqualTo(403);
        assertThat(api.post(token, "/api/admin/accounts/{id}/reset-password", null, someUserId).status()).isEqualTo(403);
        assertThat(api.get(token, "/api/admin/audit-logs").status()).isEqualTo(403);
        assertThat(api.get(token, "/api/admin/reports/tenants?month=2026-09").status()).isEqualTo(403);

        assertThat(ownerJdbc().queryForObject("SELECT tenant_enabled FROM mt_tenant WHERE tenant_id = ?", Boolean.class, tenantB.tenantId()))
                .isTrue();
    }

    @Test
    void adminAccountListCanBeFilteredByTenant() {
        Response response = api.get(adminToken(), "/api/admin/accounts?tenantId={id}", tenantA.tenantId());

        assertThat(response.status()).isEqualTo(200);
        List<String> accounts = new ArrayList<>();
        response.body().forEach(account -> {
            assertThat(account.get("tenantId").asText()).isEqualTo(tenantA.tenantId());
            accounts.add(account.get("account").asText());
        });
        assertThat(accounts).containsExactly(tenantA.account());
    }

    @Test
    void disablingATenantLogsOutOnlyThatTenantOnTheNextRequest() {
        ApiTestClient.LoginResult loginOfA = api.login(tenantA.account(), tenantA.password());
        assertThat(api.get(loginOfA.accessToken(), "/api/me").status()).isEqualTo(200);

        assertThat(api.post(adminToken(), "/api/admin/tenants/{id}/disable", null, tenantA.tenantId()).status()).isEqualTo(200);

        assertThat(api.get(loginOfA.accessToken(), "/api/me").status()).isEqualTo(401);
        assertThat(api.get(loginOfA.accessToken(), CUSTOMERS).status()).isEqualTo(401);
        assertThat(api.refresh(loginOfA.refreshToken()).status()).isEqualTo(401);
        assertThat(api.login(tenantA.account(), tenantA.password()).status()).isEqualTo(401);
        assertThat(api.get(tenantB.token(), "/api/me").status()).isEqualTo(200);

        assertThat(api.post(adminToken(), "/api/admin/tenants/{id}/enable", null, tenantA.tenantId()).status()).isEqualTo(200);
        assertThat(api.login(tenantA.account(), tenantA.password()).status()).isEqualTo(200);
    }

    private static List<String> customerIds(Response response) {
        assertThat(response.status()).isEqualTo(200);
        return response.body().get("items").findValuesAsText("customerId");
    }
}
