package com.example.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.starter.support.ApiTestClient;
import com.example.starter.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <code>AuditLogIT</code> 操作紀錄：
 * 每種操作（新增、修改、刪除、作廢、啟用／停用、重設密碼）都留下紀錄；租戶帳號讀不到操作紀錄。
 */
class AuditLogIT extends IntegrationTestBase {

    private ApiTestClient api;

    private TenantFixture tenant;

    private String adminToken;

    @BeforeEach
    void setUp() {
        api = api();
        adminToken = adminToken();
        tenant = newTenantWithAccount("AuditA");
    }

    @Test
    void creatingATenantAndAnAccountIsRecorded() {
        assertThat(actions(tenant.tenantId())).contains("CREATE");
        assertThat(tenantOf(tenant.tenantId(), "CREATE")).isNull();
        String userId = api.get(tenant.token(), "/api/me").body().get("userId").asText();
        assertThat(actions(userId)).contains("CREATE", "UPDATE");
        // 帳號自己改密碼：記在自己的租戶，內容不含密碼
        assertThat(changes(userId, "UPDATE")).doesNotContain(tenant.password());
    }

    @Test
    void createUpdateAndDeleteOfACustomerKeepTheContentBeforeDeletion() {
        String customerId = createCustomer(tenant.token(), "操作紀錄客戶");
        api.put(tenant.token(), "/api/customers/{id}", Map.of("customerName", "改名後", "customerCode", "C-1"), customerId);
        assertThat(api.delete(tenant.token(), "/api/customers/{id}", customerId).status()).isEqualTo(204);

        assertThat(actions(customerId)).containsExactlyInAnyOrder("CREATE", "UPDATE", "DELETE");
        JsonNode update = json(changes(customerId, "UPDATE"));
        assertThat(update.get("名稱").get("from").asText()).isEqualTo("操作紀錄客戶");
        assertThat(update.get("名稱").get("to").asText()).isEqualTo("改名後");
        JsonNode delete = json(changes(customerId, "DELETE"));
        assertThat(delete.get("名稱").get("from").asText()).isEqualTo("改名後");
        assertThat(delete.get("代碼").get("from").asText()).isEqualTo("C-1");
        assertThat(delete.get("名稱").has("to")).isFalse();
        assertThat(tenantOf(customerId, "DELETE")).isEqualTo(tenant.tenantId());
        assertThat(operatorOf(customerId, "DELETE")).isEqualTo(api.get(tenant.token(), "/api/me").body().get("userId").asText());
    }

    @Test
    void creatingAndVoidingAnOrderIsRecorded() {
        String customerId = createCustomer(tenant.token(), "訂單客戶");
        String orderId = createOrder(tenant.token(), customerId);
        api.post(tenant.token(), "/api/orders/{id}/void", Map.of("reason", "測試作廢"), orderId);

        assertThat(actions(orderId)).containsExactlyInAnyOrder("CREATE", "VOID");
        JsonNode created = json(changes(orderId, "CREATE"));
        assertThat(created.get("客戶").get("to").asText()).isEqualTo("訂單客戶");
        assertThat(created.get("小計").get("to").asText()).isEqualTo("200");
        assertThat(json(changes(orderId, "VOID")).get("作廢原因").get("to").asText()).isEqualTo("測試作廢");
        assertThat(labelOf(orderId, "VOID")).isEqualTo("訂單客戶 測試品項 2026-09-10");
    }

    @Test
    void enableDisableAndResetPasswordAreRecorded() {
        String userId = api.get(tenant.token(), "/api/me").body().get("userId").asText();
        String temporaryPassword = api.postJson(adminToken, "/api/admin/accounts/" + userId + "/reset-password", null, 200)
                .get("temporaryPassword").asText();
        assertThat(actions(userId)).contains("RESET_PASSWORD");
        assertThat(changes(userId, "RESET_PASSWORD")).doesNotContain(temporaryPassword);

        api.post(adminToken, "/api/admin/tenants/{id}/disable", null, tenant.tenantId());
        api.post(adminToken, "/api/admin/tenants/{id}/enable", null, tenant.tenantId());
        assertThat(actions(tenant.tenantId())).containsExactlyInAnyOrder("CREATE", "DISABLE", "ENABLE");
    }

    @Test
    void failedOperationsLeaveNoRecord() {
        String customerId = createCustomer(tenant.token(), "有訂單");
        createOrder(tenant.token(), customerId);
        assertThat(api.delete(tenant.token(), "/api/customers/{id}", customerId).status()).isEqualTo(409);
        api.postJson(tenant.token(), "/api/customers", Map.of("customerName", "代碼重複", "customerCode", "DUP-1"), 200);
        assertThat(api.post(tenant.token(), "/api/customers", Map.of("customerName", "代碼重複", "customerCode", "DUP-1")).status())
                .isEqualTo(409);

        assertThat(actions(customerId)).containsExactly("CREATE");
        assertThat(ownerJdbc().queryForObject("SELECT count(*) FROM mt_audit_log WHERE record_label = '代碼重複' AND tenant_id = ?", Long.class,
                                              tenant.tenantId())).isEqualTo(1L);
    }

    @Test
    void onlySysAdminCanReadAuditLogs() throws SQLException {
        createCustomer(tenant.token(), "總管理員看得到");

        JsonNode page = api.get(adminToken, "/api/admin/audit-logs?tenantId={id}&size=100", tenant.tenantId()).body();
        assertThat(page.get("total").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(page.get("items").findValuesAsText("recordLabel")).contains("總管理員看得到");
        JsonNode item = page.get("items").get(0);
        assertThat(item.get("tenantName").asText()).isEqualTo(tenant.tenantName());
        assertThat(item.get("operatorAccount").asText()).isEqualTo(tenant.account());

        assertThat(api.get(tenant.token(), "/api/admin/audit-logs").status()).isEqualTo(403);

        // 資料庫層：租戶的交易連自己租戶的操作紀錄都讀不到，也不能替別人寫入、不能刪除
        try (Connection connection = appDataSource().getConnection()) {
            connection.setAutoCommit(false);
            setTenant(connection, tenant.tenantId());
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM mt_audit_log")) {
                resultSet.next();
                assertThat(resultSet.getLong(1)).isZero();
            }
            assertThatThrownBy(() -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO mt_audit_log (tenant_id, operator_id, audit_action, table_name, record_id, create_time, create_user) "
                                                    + "VALUES (NULL, 'x', 'CREATE', 'mt_customer', 'x', 0, 'x')");
                }
            }).isInstanceOf(SQLException.class);
            connection.rollback();
            setTenant(connection, tenant.tenantId());
            assertThatThrownBy(() -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM mt_audit_log");
                }
            }).isInstanceOf(SQLException.class);
            connection.rollback();
        }
    }

    private static void setTenant(Connection connection, String tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.tenant_id', ?, true), set_config('app.all_tenants', 'off', true)")) {
            statement.setString(1, tenantId);
            statement.execute();
        }
    }

    private List<String> actions(String recordId) {
        return ownerJdbc().queryForList("SELECT audit_action FROM mt_audit_log WHERE record_id = ? ORDER BY create_time, audit_log_id",
                                        String.class, recordId);
    }

    private String changes(String recordId, String action) {
        return ownerJdbc().queryForObject("SELECT changes::text FROM mt_audit_log WHERE record_id = ? AND audit_action = ? LIMIT 1",
                                          String.class, recordId, action);
    }

    private String labelOf(String recordId, String action) {
        return ownerJdbc().queryForObject("SELECT record_label FROM mt_audit_log WHERE record_id = ? AND audit_action = ? LIMIT 1",
                                          String.class, recordId, action);
    }

    private String tenantOf(String recordId, String action) {
        return ownerJdbc().queryForObject("SELECT tenant_id FROM mt_audit_log WHERE record_id = ? AND audit_action = ? LIMIT 1",
                                          String.class, recordId, action);
    }

    private String operatorOf(String recordId, String action) {
        return ownerJdbc().queryForObject("SELECT operator_id FROM mt_audit_log WHERE record_id = ? AND audit_action = ? LIMIT 1",
                                          String.class, recordId, action);
    }

    private static JsonNode json(String text) {
        try {
            return new ObjectMapper().readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
