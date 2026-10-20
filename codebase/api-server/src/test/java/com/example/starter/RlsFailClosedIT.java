package com.example.starter;

import com.example.starter.business.customer.model.CustomerInput;
import com.example.starter.business.customer.service.CustomerService;
import com.example.starter.support.IntegrationTestBase;
import com.example.starter.support.NativeSqlCounter;
import com.example.starter.user.tenant.TenantContext;
import com.example.starter.user.tenant.TenantState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <code>RlsFailClosedIT</code> 資料庫層（PostgreSQL Row-Level Security）的隔離保證：
 * <ul>
 *     <li>沒有設定目前租戶的交易查不到任何資料（故障時是「看不到」，不是「看到別人的」）</li>
 *     <li>總管理員新增、修改、刪除業務資料表都被資料庫拒絕</li>
 *     <li>手寫 SQL（不經過 Hibernate 的租戶條件）一樣被 Row-Level Security 限制</li>
 *     <li>訂單的客戶必須是同一個租戶的（複合外鍵）；訂單只能作廢、不能修改（trigger）</li>
 * </ul>
 * 這裡直接以後端執行用帳號 starter_app 連線、手動控制交易與設定，不經過任何後端程式的權限檢查。
 */
class RlsFailClosedIT extends IntegrationTestBase {

    private static final String INSUFFICIENT_PRIVILEGE = "42501";

    private static final String CHECK_VIOLATION = "23514";

    @Autowired
    private CustomerService customerService;

    @Autowired
    private NativeSqlCounter nativeSqlCounter;

    private String tenantA;

    private String tenantB;

    private String customerOfA;

    private String customerOfB;

    @BeforeEach
    void seed() {
        JdbcTemplate owner = ownerJdbc();
        tenantA = insertTenant(owner, uniqueName("RlsA"));
        tenantB = insertTenant(owner, uniqueName("RlsB"));
        customerOfA = insertCustomer(owner, tenantA, "A 原始內容");
        customerOfB = insertCustomer(owner, tenantB, "B 原始內容");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void appAccountIsNeitherSuperuserNorTableOwner() throws SQLException {
        try (Connection connection = appDataSource().getConnection()) {
            assertThat(queryStrings(connection, "SELECT current_user")).containsExactly("starter_app");
            assertThat(queryStrings(connection, "SELECT rolsuper::text FROM pg_roles WHERE rolname = current_user")).containsExactly("false");
            assertThat(queryStrings(connection, "SELECT rolbypassrls::text FROM pg_roles WHERE rolname = current_user")).containsExactly("false");
            assertThat(queryStrings(connection, "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tableowner = current_user")).isEmpty();
            for (String table : List.of("mt_tenant", "mt_customer", "mt_sales_order", "mt_audit_log")) {
                assertThat(queryStrings(connection, "SELECT relrowsecurity::text || relforcerowsecurity::text FROM pg_class WHERE relname = '" + table + "'"))
                        .as(table).containsExactly("truetrue");
            }
            // 只有客戶可以刪除（沒有訂單時）；訂單只能作廢、操作紀錄只能新增
            assertThat(queryStrings(connection, "SELECT has_table_privilege(current_user, 'mt_customer', 'DELETE')::text")).containsExactly("true");
            for (String table : List.of("mt_sales_order", "mt_audit_log", "mt_tenant", "mt_user")) {
                assertThat(queryStrings(connection, "SELECT has_table_privilege(current_user, '" + table + "', 'DELETE')::text"))
                        .as(table).containsExactly("false");
            }
        }
    }

    @Test
    void transactionWithoutCurrentTenantSeesNothingAndCannotWrite() throws SQLException {
        try (Connection connection = appConnection()) {
            assertThat(queryStrings(connection, "SELECT customer_id FROM mt_customer")).isEmpty();
            assertThat(queryStrings(connection, "SELECT customer_id FROM mt_customer WHERE customer_id = '" + customerOfA + "'")).isEmpty();
            assertThat(executeUpdate(connection, "UPDATE mt_customer SET customer_name = 'x'")).isZero();
            assertThat(executeUpdate(connection, "DELETE FROM mt_customer")).isZero();
            assertInsufficientPrivilege(() -> insertCustomerAsApp(connection, tenantA));
            connection.rollback();
        }
        assertThat(nameOf(customerOfA)).isEqualTo("A 原始內容");
    }

    @Test
    void sysAdminSettingCanReadAllTenantsButCannotInsertUpdateOrDelete() throws SQLException {
        try (Connection connection = appConnection()) {
            setTenant(connection, "", "on");
            assertThat(queryStrings(connection, "SELECT customer_id FROM mt_customer")).contains(customerOfA, customerOfB);
            assertThat(executeUpdate(connection, "UPDATE mt_customer SET customer_name = '總管理員修改'")).isZero();
            assertThat(executeUpdate(connection, "DELETE FROM mt_customer")).isZero();
            connection.commit();

            setTenant(connection, "", "on");
            assertInsufficientPrivilege(() -> insertCustomerAsApp(connection, tenantA));
            connection.rollback();
        }
        assertThat(nameOf(customerOfA)).isEqualTo("A 原始內容");
        assertThat(nameOf(customerOfB)).isEqualTo("B 原始內容");
    }

    @Test
    void handwrittenSqlIsLimitedToCurrentTenant() throws SQLException {
        try (Connection connection = appConnection()) {
            setTenant(connection, tenantA, "off");

            assertThat(queryStrings(connection, "SELECT DISTINCT tenant_id FROM mt_customer")).containsExactly(tenantA);
            assertThat(queryStrings(connection, "SELECT customer_id FROM mt_customer WHERE customer_id = '" + customerOfB + "'")).isEmpty();
            assertThat(executeUpdate(connection, "DELETE FROM mt_customer WHERE customer_id = '" + customerOfB + "'")).isZero();
            connection.commit();

            // 不能替其他租戶新增，也不能把自己的資料「搬」到其他租戶
            setTenant(connection, tenantA, "off");
            assertInsufficientPrivilege(() -> insertCustomerAsApp(connection, tenantB));
            connection.rollback();

            setTenant(connection, tenantA, "off");
            assertInsufficientPrivilege(() -> executeUpdate(connection,
                    "UPDATE mt_customer SET tenant_id = '" + tenantB + "' WHERE customer_id = '" + customerOfA + "'"));
            connection.rollback();
        }
        assertThat(nameOf(customerOfB)).isEqualTo("B 原始內容");
        assertThat(ownerJdbc().queryForObject("SELECT tenant_id FROM mt_customer WHERE customer_id = ?", String.class, customerOfA))
                .isEqualTo(tenantA);
    }

    @Test
    void tenantTableIsReadableWithoutTenantButOnlySysAdminCanWriteIt() throws SQLException {
        try (Connection connection = appConnection()) {
            // 登入時還不知道是哪個租戶，也要讀得到帳號所屬租戶是否啟用
            assertThat(queryStrings(connection, "SELECT tenant_id FROM mt_tenant WHERE tenant_id = '" + tenantA + "'")).containsExactly(tenantA);
            assertThat(executeUpdate(connection, "UPDATE mt_tenant SET tenant_name = tenant_name || '-x' WHERE tenant_id = '" + tenantA + "'")).isZero();
            connection.rollback();

            setTenant(connection, tenantA, "off");
            assertThat(executeUpdate(connection, "UPDATE mt_tenant SET tenant_enabled = FALSE WHERE tenant_id = '" + tenantA + "'")).isZero();
            assertInsufficientPrivilege(() -> executeUpdate(connection,
                    "INSERT INTO mt_tenant (tenant_name, create_time, create_user) VALUES ('" + uniqueName("ByTenant") + "', 0, 'test')"));
            connection.rollback();

            setTenant(connection, "", "on");
            assertThat(executeUpdate(connection, "UPDATE mt_tenant SET update_time = 1 WHERE tenant_id = '" + tenantA + "'")).isEqualTo(1);
            connection.rollback();
        }
        assertThat(ownerJdbc().queryForObject("SELECT tenant_enabled FROM mt_tenant WHERE tenant_id = ?", Boolean.class, tenantA)).isTrue();
    }

    @Test
    void currentTenantSettingDoesNotLeakToTheNextTransaction() throws SQLException {
        try (Connection connection = appConnection()) {
            setTenant(connection, tenantA, "off");
            assertThat(queryStrings(connection, "SELECT customer_id FROM mt_customer")).contains(customerOfA);
            connection.commit();

            assertThat(queryStrings(connection, "SELECT customer_id FROM mt_customer")).isEmpty();
            connection.rollback();
        }
    }

    @Test
    void applicationTransactionManagerAppliesCurrentTenantToHandwrittenSql() {
        long rowsOfA = ownerJdbc().queryForObject("SELECT count(*) FROM mt_customer WHERE tenant_id = ?", Long.class, tenantA);
        long allRows = ownerJdbc().queryForObject("SELECT count(*) FROM mt_customer", Long.class);

        TenantContext.set(TenantState.tenant("test-user", tenantA));
        assertThat(nativeSqlCounter.count("mt_customer")).isEqualTo(rowsOfA);

        TenantContext.set(TenantState.none());
        assertThat(nativeSqlCounter.count("mt_customer")).isZero();

        TenantContext.set(TenantState.allTenants("test-admin"));
        assertThat(nativeSqlCounter.count("mt_customer")).isEqualTo(allRows);

        TenantContext.clear();
        assertThat(nativeSqlCounter.count("mt_customer")).isZero();
    }

    @Test
    void sysAdminWritesThroughTheApplicationAreRejectedByTheDatabase() {
        // 就算 Controller 的角色檢查被拿掉，總管理員的交易寫入業務資料也會被資料庫擋下
        TenantContext.set(TenantState.allTenants("test-admin"));
        CustomerInput input = new CustomerInput("總管理員新增", null, null, null);

        assertThatThrownBy(() -> customerService.create(input, "test-admin")).satisfies(this::assertCausedByInsufficientPrivilegeOrNoRows);
        assertThatThrownBy(() -> customerService.update(customerOfA, new CustomerInput("總管理員修改", null, null, null), "test-admin"))
                .satisfies(this::assertCausedByInsufficientPrivilegeOrNoRows);
        assertThatThrownBy(() -> customerService.delete(customerOfA)).satisfies(this::assertCausedByInsufficientPrivilegeOrNoRows);

        assertThat(nameOf(customerOfA)).isEqualTo("A 原始內容");
        assertThat(ownerJdbc().queryForObject("SELECT count(*) FROM mt_customer WHERE customer_name = '總管理員新增'", Long.class)).isZero();
    }

    @Test
    void orderMustUseACustomerOfTheSameTenant() {
        // 即使是資料表擁有者（superuser，不受 RLS 限制），複合外鍵也不允許 A 的訂單掛在 B 的客戶上
        assertThatThrownBy(() -> insertOrder(ownerJdbc(), tenantA, customerOfB))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void ordersFollowRowLevelSecurityAndCanOnlyBeVoided() throws SQLException {
        String orderOfA = insertOrder(ownerJdbc(), tenantA, customerOfA);
        String voidSql = "UPDATE mt_sales_order SET order_status = 'VOIDED', void_reason = '測試', void_time = 1, void_user = 'x' "
                + "WHERE order_id = '" + orderOfA + "'";
        try (Connection connection = appConnection()) {
            // 沒有設定租戶：看不到、改不到
            assertThat(queryStrings(connection, "SELECT order_id FROM mt_sales_order")).isEmpty();
            assertThat(executeUpdate(connection, voidSql)).isZero();
            connection.rollback();

            // 總管理員：看得到，不能作廢
            setTenant(connection, "", "on");
            assertThat(queryStrings(connection, "SELECT order_id FROM mt_sales_order WHERE order_id = '" + orderOfA + "'")).containsExactly(orderOfA);
            assertThat(executeUpdate(connection, voidSql)).isZero();
            connection.rollback();

            // 租戶自己：不能修改訂單內容（trigger），只能作廢
            setTenant(connection, tenantA, "off");
            assertThatThrownBy(() -> executeUpdate(connection, "UPDATE mt_sales_order SET quantity = 9 WHERE order_id = '" + orderOfA + "'"))
                    .isInstanceOf(SQLException.class)
                    .satisfies(ex -> assertThat(((SQLException) ex).getSQLState()).isEqualTo(CHECK_VIOLATION));
            connection.rollback();

            setTenant(connection, tenantA, "off");
            assertThat(executeUpdate(connection, voidSql)).isEqualTo(1);
            // 作廢後不能再改回正常
            assertThatThrownBy(() -> executeUpdate(connection, "UPDATE mt_sales_order SET order_status = 'NORMAL' WHERE order_id = '" + orderOfA + "'"))
                    .isInstanceOf(SQLException.class);
            connection.rollback();
        }
        assertThat(ownerJdbc().queryForObject("SELECT order_status FROM mt_sales_order WHERE order_id = ?", String.class, orderOfA))
                .isEqualTo("NORMAL");
    }

    /**
     * 新增被 RLS 拒絕時是 SQLState 42501；修改、刪除時 RLS 讓資料「看不到」，影響 0 筆，Hibernate 回報資料已不存在
     */
    private void assertCausedByInsufficientPrivilegeOrNoRows(Throwable thrown) {
        List<String> chain = new ArrayList<>();
        boolean rejected = false;
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            chain.add(cause.getClass().getSimpleName());
            if (cause instanceof SQLException sqlException && INSUFFICIENT_PRIVILEGE.equals(sqlException.getSQLState())) {
                rejected = true;
            }
            if (cause.getClass().getSimpleName().contains("OptimisticLock") || cause.getClass().getSimpleName().contains("StaleObjectState")) {
                rejected = true;
            }
        }
        assertThat(rejected).as("exception chain: %s", chain).isTrue();
    }

    private Connection appConnection() throws SQLException {
        Connection connection = appDataSource().getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private static void setTenant(Connection connection, String tenantId, String allTenants) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.tenant_id', ?, true), set_config('app.all_tenants', ?, true)")) {
            statement.setString(1, tenantId);
            statement.setString(2, allTenants);
            statement.execute();
        }
    }

    private static List<String> queryStrings(Connection connection, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
        }
        return values;
    }

    private static int executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private static int insertCustomerAsApp(Connection connection, String tenantId) throws SQLException {
        return executeUpdate(connection, "INSERT INTO mt_customer (tenant_id, customer_name, create_time, create_user) VALUES ('"
                + tenantId + "', '不應該寫入', 0, 'test')");
    }

    private static void assertInsufficientPrivilege(SqlAction action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(SQLException.class)
                .satisfies(ex -> assertThat(((SQLException) ex).getSQLState()).isEqualTo(INSUFFICIENT_PRIVILEGE));
    }

    @FunctionalInterface
    private interface SqlAction {
        Object run() throws SQLException;
    }

    private static String insertTenant(JdbcTemplate owner, String name) {
        String id = UUID.randomUUID().toString();
        owner.update("INSERT INTO mt_tenant (tenant_id, tenant_name, create_time, create_user) VALUES (?, ?, 0, 'test')", id, name);
        return id;
    }

    private static String insertCustomer(JdbcTemplate owner, String tenantId, String name) {
        String id = UUID.randomUUID().toString();
        owner.update("INSERT INTO mt_customer (customer_id, tenant_id, customer_name, create_time, create_user) VALUES (?, ?, ?, 0, 'test')",
                     id, tenantId, name);
        return id;
    }

    private static String insertOrder(JdbcTemplate owner, String tenantId, String customerId) {
        String id = UUID.randomUUID().toString();
        owner.update("INSERT INTO mt_sales_order (order_id, tenant_id, customer_id, order_date, item_name, unit_price, quantity, subtotal, "
                             + "create_time, create_user) VALUES (?, ?, ?, DATE '2026-09-05', '品項', 100, 2, 200, 0, 'test')",
                     id, tenantId, customerId);
        return id;
    }

    private String nameOf(String customerId) {
        return ownerJdbc().queryForObject("SELECT customer_name FROM mt_customer WHERE customer_id = ?", String.class, customerId);
    }
}
