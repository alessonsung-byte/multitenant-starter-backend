package com.example.starter.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>IntegrationTestBase</code> 所有整合測試（*IT）的共用設定。
 * <br>
 * 以 Testcontainers 啟動真的 PostgreSQL 與 Redis（同一次測試執行只啟動一次，所有測試類別共用），並且和正式環境一樣：
 * <ul>
 *     <li>Flyway 以建表帳號（資料表擁有者、superuser）建立資料表</li>
 *     <li>後端以 starter_app 帳號連線（不是擁有者，Row-Level Security 一定生效）</li>
 * </ul>
 * 因為資料庫由所有測試共用，每個測試都以隨機名稱建立自己的租戶與帳號，不依賴其他測試留下的資料。
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    protected static final String ADMIN_ACCOUNT = "admin";

    /**
     * 測試開始時把預設總管理員的密碼改成這個（V2 migration 規定 admin 第一次登入就要改密碼）
     */
    protected static final String ADMIN_TEST_PASSWORD = "Admin-Test-2026";

    private static final String APP_DB_USER = "starter_app";

    private static final String APP_DB_PASSWORD = "starter_app_test";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.5")
            .withDatabaseName("starter")
            .withUsername("starter_owner")
            .withPassword("starter_owner_test")
            .withInitScript("db/create-app-role.sql");

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2").withExposedPorts(6379);

    private static final String JWT_KEY;

    private static boolean adminPasswordChanged;

    static {
        POSTGRES.start();
        REDIS.start();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        JWT_KEY = Base64.getEncoder().encodeToString(key);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_DB_USER);
        registry.add("spring.datasource.password", () -> APP_DB_PASSWORD);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("user.jwt.token.signing.key", () -> JWT_KEY);
        registry.add("jasypt.encryptor.password", () -> "integration-test");
        // 測試時降低 BCrypt 強度，縮短測試時間（不影響密碼規則本身）
        registry.add("bcrypt.password.encoder.strength", () -> "4");
    }

    protected ApiTestClient api() {
        return new ApiTestClient(mockMvc, objectMapper);
    }

    /**
     * 以資料表擁有者身分直接連線（不受 Row-Level Security 限制），只用來準備測試資料、檢查資料庫實際內容
     */
    protected JdbcTemplate ownerJdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    /**
     * 測試資料庫的 JDBC 網址（需要另外建立資料庫的測試使用）
     */
    protected String ownerJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    /**
     * 以後端執行用帳號 starter_app 直接連線的設定
     */
    protected DriverManagerDataSource appDataSource() {
        return new DriverManagerDataSource(POSTGRES.getJdbcUrl(), APP_DB_USER, APP_DB_PASSWORD);
    }

    protected static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 總管理員的 access token。第一次呼叫時把預設密碼 admin 改成 {@link #ADMIN_TEST_PASSWORD}
     */
    protected synchronized String adminToken() {
        ApiTestClient api = api();
        if (!adminPasswordChanged) {
            ApiTestClient.LoginResult first = api.login(ADMIN_ACCOUNT, "admin");
            if (first.status() == 200) {
                assertThat(first.body().get("mustChangePassword").asBoolean()).isTrue();
                assertThat(api.changePassword(first.accessToken(), ADMIN_ACCOUNT, "admin", ADMIN_TEST_PASSWORD)).isEqualTo(204);
            }
            adminPasswordChanged = true;
        }
        return api.loginToken(ADMIN_ACCOUNT, ADMIN_TEST_PASSWORD);
    }

    /**
     * 以總管理員身分新增一個租戶，回傳租戶編號
     */
    protected String createTenant(String adminToken, String tenantName) {
        JsonNode body = api().postJson(adminToken, "/api/admin/tenants", Map.of("tenantName", tenantName), 200);
        return body.get("tenantId").asText();
    }

    /**
     * 以總管理員身分新增一個租戶帳號，回傳臨時密碼
     */
    protected String createTenantAccount(String adminToken, String tenantId, String account) {
        JsonNode body = api().postJson(adminToken, "/api/admin/accounts",
                                       Map.of("account", account, "roleName", "TENANT_USER", "tenantId", tenantId), 200);
        return body.get("temporaryPassword").asText();
    }

    /**
     * 新帳號第一次登入：用臨時密碼登入、改成新密碼，再用新密碼登入，回傳 access token
     */
    protected String activateAccount(String account, String temporaryPassword, String newPassword) {
        ApiTestClient api = api();
        String firstToken = api.loginToken(account, temporaryPassword);
        assertThat(api.changePassword(firstToken, account, temporaryPassword, newPassword)).isEqualTo(204);
        return api.loginToken(account, newPassword);
    }

    /**
     * 建立一個租戶與一個已完成首次改密碼的租戶帳號
     */
    protected TenantFixture newTenantWithAccount(String prefix) {
        String adminToken = adminToken();
        String tenantName = uniqueName(prefix);
        String tenantId = createTenant(adminToken, tenantName);
        String account = uniqueName(prefix.toLowerCase());
        String temporaryPassword = createTenantAccount(adminToken, tenantId, account);
        String password = "Pw-" + UUID.randomUUID().toString().substring(0, 12);
        String token = activateAccount(account, temporaryPassword, password);
        return new TenantFixture(tenantId, tenantName, account, password, token);
    }

    /**
     * 以租戶帳號新增一位客戶，回傳客戶編號
     */
    protected String createCustomer(String tenantToken, String customerName) {
        return api().postJson(tenantToken, "/api/customers", Map.of("customerName", customerName), 200).get("customerId").asText();
    }

    /**
     * 以租戶帳號替客戶新增一筆訂單（單價 100、數量 2），回傳訂單編號
     */
    protected String createOrder(String tenantToken, String customerId) {
        return createOrder(tenantToken, customerId, "2026-09-10", "100", 2);
    }

    protected String createOrder(String tenantToken, String customerId, String orderDate, String unitPrice, int quantity) {
        return api().postJson(tenantToken, "/api/orders", Map.of("customerId", customerId, "orderDate", orderDate, "itemName", "測試品項",
                                                                  "unitPrice", new java.math.BigDecimal(unitPrice), "quantity", quantity), 200)
                .get("orderId").asText();
    }

    /**
     * 測試用的一個租戶與它的帳號
     */
    public record TenantFixture(String tenantId, String tenantName, String account, String password, String token) {
    }
}
