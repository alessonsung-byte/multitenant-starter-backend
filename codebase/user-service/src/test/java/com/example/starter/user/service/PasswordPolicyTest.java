package com.example.starter.user.service;

import com.example.starter.common.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <code>PasswordPolicyTest</code> 密碼規則：至少 8 碼、不可與帳號相同、不可與舊密碼相同；
 * 以及總管理員產生的臨時密碼一定符合規則
 */
class PasswordPolicyTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private final PasswordPolicy passwordPolicy = new PasswordPolicy(passwordEncoder);

    @Test
    void passwordWithEightCharactersIsAccepted() {
        assertThatCode(() -> passwordPolicy.validate("tenant01", "abcd1234", null)).doesNotThrowAnyException();
    }

    @Test
    void passwordShorterThanEightCharactersIsRejected() {
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "abc1234", null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("密碼至少需要 8 個字元");
    }

    @Test
    void emptyPasswordIsRejected() {
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", null, null)).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "", null)).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void passwordSameAsAccountIsRejectedRegardlessOfCase() {
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "tenant01", null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("密碼不可與帳號相同");
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "TENANT01", null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("密碼不可與帳號相同");
    }

    @Test
    void passwordContainingTheAccountButNotEqualIsAccepted() {
        assertThatCode(() -> passwordPolicy.validate("tenant01", "tenant01-2026", null)).doesNotThrowAnyException();
    }

    @Test
    void passwordSameAsTheCurrentPasswordIsRejected() {
        String currentHash = passwordEncoder.encode("old-password-1");

        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "old-password-1", currentHash))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("新密碼不可與舊密碼相同");
        assertThatCode(() -> passwordPolicy.validate("tenant01", "new-password-1", currentHash)).doesNotThrowAnyException();
    }

    @Test
    void passwordLongerThanBcryptCanHandleIsRejected() {
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "a".repeat(73), null)).isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> passwordPolicy.validate("tenant01", "密".repeat(25), null)).isInstanceOf(InvalidArgumentException.class);
        assertThatCode(() -> passwordPolicy.validate("tenant01", "a".repeat(72), null)).doesNotThrowAnyException();
    }

    @Test
    void temporaryPasswordsHaveTwelveUnambiguousCharactersAndFollowThePolicy() {
        TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();
        Set<String> generated = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            String password = generator.generate();
            assertThat(password).hasSize(12).matches("[A-HJ-NP-Za-km-np-z2-9]+");
            assertThatCode(() -> passwordPolicy.validate("tenant01", password, null)).doesNotThrowAnyException();
            generated.add(password);
        }
        assertThat(generated).hasSize(200);
    }
}
