package com.example.starter;

import com.example.starter.support.ApiTestClient;
import com.example.starter.support.ApiTestClient.LoginResult;
import com.example.starter.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <code>LoginLockoutIT</code> 登入失敗次數限制：
 * 同一帳號連續失敗 5 次就鎖定；鎖定期間連正確密碼也登入不了，而且回覆與密碼錯誤完全相同；鎖定時間到或總管理員重設密碼後解鎖。
 * <br>
 * 正式設定的鎖定時間是 15 分鐘；這裡縮短為 2 秒，才能在測試中驗證「時間到自動解鎖」。
 */
@TestPropertySource(properties = "login.lock.duration.seconds=2")
class LoginLockoutIT extends IntegrationTestBase {

    private static final String WRONG_PASSWORD = "wrong-password";

    private static final String FAILED_MESSAGE = "帳號或密碼錯誤";

    private ApiTestClient api;

    private TenantFixture tenant;

    @BeforeEach
    void setUp() {
        api = api();
        tenant = newTenantWithAccount("Lockout");
    }

    @Test
    void fiveConsecutiveFailuresLockTheAccountEvenForTheCorrectPassword() {
        for (int i = 1; i <= 5; i++) {
            LoginResult result = api.login(tenant.account(), WRONG_PASSWORD);
            assertThat(result.status()).isEqualTo(401);
            assertThat(result.message()).isEqualTo(FAILED_MESSAGE);
        }

        LoginResult locked = api.login(tenant.account(), tenant.password());

        assertThat(locked.status()).isEqualTo(401);
        assertThat(locked.message()).isEqualTo(FAILED_MESSAGE);
        assertThat(locked.body().get("code").asText()).isEqualTo("ERROR_USER_LOGIN");
    }

    @Test
    void fourFailuresDoNotLockTheAccount() {
        for (int i = 1; i <= 4; i++) {
            assertThat(api.login(tenant.account(), WRONG_PASSWORD).status()).isEqualTo(401);
        }

        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(200);
    }

    @Test
    void successfulLoginResetsTheFailureCount() {
        for (int i = 1; i <= 4; i++) {
            api.login(tenant.account(), WRONG_PASSWORD);
        }
        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(200);
        for (int i = 1; i <= 4; i++) {
            api.login(tenant.account(), WRONG_PASSWORD);
        }

        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(200);
    }

    @Test
    void lockIsReleasedAutomaticallyWhenTheLockTimeIsOver() throws InterruptedException {
        lock(tenant.account());
        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(401);

        Thread.sleep(2500);

        assertThat(api.login(tenant.account(), tenant.password()).status()).isEqualTo(200);
    }

    @Test
    void adminPasswordResetReleasesTheLock() {
        lock(tenant.account());
        String userId = userIdOf(tenant.account());

        String temporaryPassword = api.postJson(adminToken(), "/api/admin/accounts/" + userId + "/reset-password", null, 200)
                .get("temporaryPassword").asText();

        LoginResult result = api.login(tenant.account(), temporaryPassword);
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().get("mustChangePassword").asBoolean()).isTrue();
    }

    @Test
    void unknownAccountGetsTheSameMessageAndIsAlsoLocked() {
        String unknown = uniqueName("nobody");
        for (int i = 1; i <= 6; i++) {
            LoginResult result = api.login(unknown, WRONG_PASSWORD);
            assertThat(result.status()).isEqualTo(401);
            assertThat(result.message()).isEqualTo(FAILED_MESSAGE);
        }
    }

    @Test
    void lockOfOneAccountDoesNotAffectOtherAccounts() {
        TenantFixture other = newTenantWithAccount("LockoutOther");
        lock(tenant.account());

        assertThat(api.login(other.account(), other.password()).status()).isEqualTo(200);
    }

    private void lock(String account) {
        for (int i = 1; i <= 5; i++) {
            api.login(account, WRONG_PASSWORD);
        }
    }

    private String userIdOf(String account) {
        return ownerJdbc().queryForObject("SELECT user_id FROM mt_user WHERE user_account = ?", String.class, account);
    }
}
