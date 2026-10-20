package com.example.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.starter.support.ApiTestClient;
import com.example.starter.support.ApiTestClient.LoginResult;
import com.example.starter.support.ApiTestClient.Response;
import com.example.starter.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>AccountManagementIT</code> 租戶與帳號管理、強制修改密碼
 */
class AccountManagementIT extends IntegrationTestBase {

    private ApiTestClient api;

    private String adminToken;

    @BeforeEach
    void setUp() {
        api = api();
        adminToken = adminToken();
    }

    @Test
    void newAccountMustChangePasswordBeforeUsingAnyOtherApi() {
        String tenantId = createTenant(adminToken, uniqueName("Must"));
        String account = uniqueName("must");
        String temporaryPassword = createTenantAccount(adminToken, tenantId, account);
        assertThat(temporaryPassword).hasSize(12);

        LoginResult first = api.login(account, temporaryPassword);
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.body().get("mustChangePassword").asBoolean()).isTrue();
        assertThat(first.body().get("tenantId").asText()).isEqualTo(tenantId);

        Response blocked = api.get(first.accessToken(), "/api/me");
        assertThat(blocked.status()).isEqualTo(403);
        assertThat(blocked.body().get("code").asText()).isEqualTo("ERROR_PASSWORD_CHANGE_REQUIRED");
        assertThat(api.get(first.accessToken(), "/api/customers").status()).isEqualTo(403);

        assertThat(api.changePassword(first.accessToken(), account, temporaryPassword, "Brand-New-Pass1")).isEqualTo(204);

        JsonNode me = api.get(first.accessToken(), "/api/me").body();
        assertThat(me.get("mustChangePassword").asBoolean()).isFalse();
        assertThat(api.login(account, temporaryPassword).status()).isEqualTo(401);
        assertThat(api.login(account, "Brand-New-Pass1").status()).isEqualTo(200);
    }

    @Test
    void passwordChangeFollowsThePasswordPolicy() {
        TenantFixture tenant = newTenantWithAccount("Policy");

        Response tooShort = api.changePasswordResponse(tenant.token(), tenant.account(), tenant.password(), "short");
        Response sameAsAccount = api.changePasswordResponse(tenant.token(), tenant.account(), tenant.password(), tenant.account().toUpperCase());
        Response sameAsOld = api.changePasswordResponse(tenant.token(), tenant.account(), tenant.password(), tenant.password());
        Response wrongCurrent = api.changePasswordResponse(tenant.token(), tenant.account(), "not-the-password", "Another-Pass-9");

        assertThat(tooShort.status()).isEqualTo(400);
        assertThat(tooShort.body().get("message").asText()).contains("8");
        assertThat(sameAsAccount.status()).isEqualTo(400);
        assertThat(sameAsAccount.body().get("message").asText()).contains("帳號");
        assertThat(sameAsOld.status()).isEqualTo(400);
        assertThat(sameAsOld.body().get("message").asText()).contains("舊密碼");
        assertThat(wrongCurrent.status()).isEqualTo(400);
        assertThat(wrongCurrent.body().get("message").asText()).isEqualTo("目前密碼錯誤");
        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(200);
    }

    @Test
    void adminResetPasswordShowsANewTemporaryPasswordAndRevokesRefreshTokens() {
        TenantFixture tenant = newTenantWithAccount("Reset");
        LoginResult session = api.login(tenant.account(), tenant.password());
        String userId = api.get(session.accessToken(), "/api/me").body().get("userId").asText();

        String temporaryPassword = api.postJson(adminToken, "/api/admin/accounts/" + userId + "/reset-password", null, 200)
                .get("temporaryPassword").asText();

        assertThat(temporaryPassword).hasSize(12).isNotEqualTo(tenant.password());
        assertThat(api.refresh(session.refreshToken()).status()).isEqualTo(401);
        assertThat(api.get(session.accessToken(), "/api/me").status()).isEqualTo(403);
        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(401);
        assertThat(api.login(tenant.account(), temporaryPassword).body().get("mustChangePassword").asBoolean()).isTrue();
        assertThat(ownerJdbc().queryForObject("SELECT count(*) FROM mt_signin WHERE signin_pw LIKE ?", Long.class, "%" + temporaryPassword + "%"))
                .isZero();
    }

    @Test
    void disabledAccountIsLoggedOutOnTheNextRequest() {
        TenantFixture tenant = newTenantWithAccount("Disable");
        String userId = api.get(tenant.token(), "/api/me").body().get("userId").asText();

        assertThat(api.post(adminToken, "/api/admin/accounts/{id}/disable", null, userId).status()).isEqualTo(200);
        assertThat(api.get(tenant.token(), "/api/me").status()).isEqualTo(401);
        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(401);

        assertThat(api.post(adminToken, "/api/admin/accounts/{id}/enable", null, userId).status()).isEqualTo(200);
        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(200);
    }

    @Test
    void duplicateNamesAndInvalidAccountsAreRejected() {
        String tenantName = uniqueName("Dup");
        String tenantId = createTenant(adminToken, tenantName);
        String account = uniqueName("dup");
        createTenantAccount(adminToken, tenantId, account);

        assertThat(api.post(adminToken, "/api/admin/tenants", Map.of("tenantName", tenantName)).status()).isEqualTo(409);
        assertThat(api.post(adminToken, "/api/admin/tenants", Map.of("tenantName", " ")).status()).isEqualTo(400);
        assertThat(api.post(adminToken, "/api/admin/accounts",
                            Map.of("account", account, "roleName", "TENANT_USER", "tenantId", tenantId)).status()).isEqualTo(409);
        assertThat(api.post(adminToken, "/api/admin/accounts",
                            Map.of("account", "有空白 的帳號", "roleName", "TENANT_USER", "tenantId", tenantId)).status()).isEqualTo(400);
        assertThat(api.post(adminToken, "/api/admin/accounts",
                            Map.of("account", uniqueName("notenant"), "roleName", "TENANT_USER")).status()).isEqualTo(400);
        assertThat(api.post(adminToken, "/api/admin/accounts",
                            Map.of("account", uniqueName("admin"), "roleName", "SYS_ADMIN", "tenantId", tenantId)).status()).isEqualTo(400);
        assertThat(api.post(adminToken, "/api/admin/accounts",
                            Map.of("account", uniqueName("bad"), "roleName", "OTHER")).status()).isEqualTo(400);
    }

    @Test
    void adminCanRenameATenantButNotToAnExistingName() {
        String first = uniqueName("Rename");
        String second = uniqueName("Rename");
        String firstId = createTenant(adminToken, first);
        createTenant(adminToken, second);

        assertThat(api.put(adminToken, "/api/admin/tenants/{id}", Map.of("tenantName", second), firstId).status()).isEqualTo(409);
        Response renamed = api.put(adminToken, "/api/admin/tenants/{id}", Map.of("tenantName", first + "-新"), firstId);
        assertThat(renamed.status()).isEqualTo(200);
        assertThat(renamed.body().get("tenantName").asText()).isEqualTo(first + "-新");
        assertThat(api.put(adminToken, "/api/admin/tenants/{id}", Map.of("tenantName", "x"), "no-such-tenant").status()).isEqualTo(404);
    }

    @Test
    void adminCannotDisableItself() {
        String adminId = api.get(adminToken, "/api/me").body().get("userId").asText();

        assertThat(api.post(adminToken, "/api/admin/accounts/{id}/disable", null, adminId).status()).isEqualTo(409);
    }

    @Test
    void cannotCreateAccountForADisabledTenant() {
        String tenantId = createTenant(adminToken, uniqueName("Off"));
        api.post(adminToken, "/api/admin/tenants/{id}/disable", null, tenantId);

        assertThat(api.post(adminToken, "/api/admin/accounts",
                            Map.of("account", uniqueName("off"), "roleName", "TENANT_USER", "tenantId", tenantId)).status()).isEqualTo(409);
    }
}
