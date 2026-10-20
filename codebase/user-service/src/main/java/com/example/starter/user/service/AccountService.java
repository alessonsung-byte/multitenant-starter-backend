package com.example.starter.user.service;

import com.example.starter.common.exception.ConflictException;
import com.example.starter.common.exception.InvalidArgumentException;
import com.example.starter.common.exception.NotFoundException;
import com.example.starter.user.entity.TenantEntity;
import com.example.starter.user.entity.RoleEntity;
import com.example.starter.user.entity.RoleUserEntity;
import com.example.starter.user.entity.RoleUserId;
import com.example.starter.user.entity.SigninEntity;
import com.example.starter.user.entity.UserEntity;
import com.example.starter.user.model.AccountModel;
import com.example.starter.user.model.TemporaryPasswordModel;
import com.example.starter.user.model.AppUserDetails;
import com.example.starter.user.repository.TenantRepository;
import com.example.starter.user.repository.RedisRefreshTokenRepository;
import com.example.starter.user.repository.RoleRepository;
import com.example.starter.user.repository.RoleUserRepository;
import com.example.starter.user.repository.SigninRepository;
import com.example.starter.user.repository.UserRepository;
import com.example.starter.user.audit.AuditAction;
import com.example.starter.user.audit.AuditRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <code>AccountService</code> 總管理員管理帳號：列表、新增、啟用／停用、重設臨時密碼。
 * <br>
 * 規則：
 * <ul>
 *     <li>一個帳號只有一個角色：總管理員（不屬於任何租戶）或租戶帳號（必須屬於一個租戶）</li>
 *     <li>新增與重設密碼時產生 12 碼臨時密碼，只回傳這一次；帳號被標記為「需要修改密碼」</li>
 *     <li>重設密碼時作廢該帳號所有 refresh token，並解除登入鎖定</li>
 *     <li>帳號只能停用、不能刪除；停用後下一次請求立即失效</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    /**
     * 帳號只能使用英數字與 . _ -，3～64 個字元（方便在手機上輸入，也避免看起來相同的字元）
     */
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,64}$");

    private static final Set<String> ROLES = Set.of(AppUserDetails.ROLE_SYS_ADMIN, AppUserDetails.ROLE_TENANT_USER);

    private final UserRepository userRepository;

    private final SigninRepository signinRepository;

    private final RoleRepository roleRepository;

    private final RoleUserRepository roleUserRepository;

    private final TenantRepository tenantRepository;

    private final PasswordEncoder passwordEncoder;

    private final PasswordPolicy passwordPolicy;

    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    private final RedisRefreshTokenRepository redisRefreshTokenRepository;

    private final LoginAttemptService loginAttemptService;

    private final AuditRecorder auditRecorder;

    /**
     * 帳號列表
     *
     * @param tenantId 只列出這個租戶的帳號；null 表示全部
     */
    @Transactional(readOnly = true)
    public List<AccountModel> findAll(String tenantId) {
        List<UserEntity> users = tenantId == null ? userRepository.findAllByOrderByCreateTimeAsc()
                : userRepository.findAllByTenantIdOrderByCreateTimeAsc(tenantId);
        Map<String, TenantEntity> tenants = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(TenantEntity::getTenantId, Function.identity()));
        return users.stream().map(user -> toModel(user, tenants.get(user.getTenantId()))).toList();
    }

    /**
     * 新增帳號，回傳一次性的臨時密碼
     *
     * @param account    登入帳號
     * @param email      信箱，可為空
     * @param roleName   SYS_ADMIN 或 TENANT_USER
     * @param tenantId   租戶帳號必填；總管理員必須是空的
     * @param operatorId 操作者的 mt_user.user_id
     */
    @Transactional
    public TemporaryPasswordModel create(String account, String email, String roleName, String tenantId, String operatorId) {
        String trimmedAccount = StringUtils.trimToEmpty(account);
        if (!ACCOUNT_PATTERN.matcher(trimmedAccount).matches()) {
            throw new InvalidArgumentException("帳號只能使用英文字母、數字與 . _ -，長度 3～64 個字元");
        }
        if (!ROLES.contains(roleName)) {
            throw new InvalidArgumentException("角色只能是總管理員或租戶帳號");
        }

        TenantEntity tenant = null;
        if (AppUserDetails.ROLE_TENANT_USER.equals(roleName)) {
            if (StringUtils.isBlank(tenantId)) {
                throw new InvalidArgumentException("租戶帳號必須選擇所屬租戶");
            }
            tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("找不到這個租戶"));
            if (!Boolean.TRUE.equals(tenant.getTenantEnabled())) {
                throw new ConflictException("這個租戶已停用，不能新增帳號");
            }
        } else if (StringUtils.isNotBlank(tenantId)) {
            throw new InvalidArgumentException("總管理員不屬於任何租戶");
        }

        if (userRepository.existsByUserAccount(trimmedAccount) || signinRepository.existsBySigninAccount(trimmedAccount)) {
            throw new ConflictException("帳號「" + trimmedAccount + "」已經存在");
        }

        RoleEntity role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
        long now = System.currentTimeMillis();

        UserEntity user = new UserEntity();
        user.setUserAccount(trimmedAccount);
        user.setUserEmail(StringUtils.trimToNull(email));
        user.setUserEnabled(true);
        user.setTenantId(tenant != null ? tenant.getTenantId() : null);
        user.setMustChangePassword(true);
        user.setCreateTime(now);
        user.setCreateUser(operatorId);
        user = userRepository.save(user);

        String temporaryPassword = newTemporaryPassword(trimmedAccount);
        SigninEntity signin = new SigninEntity();
        signin.setSigninAccount(trimmedAccount);
        signin.setSigninPw(passwordEncoder.encode(temporaryPassword));
        signin.setUserId(user.getUserId());
        signin.setCreateTime(now);
        signin.setCreateUser(operatorId);
        signinRepository.save(signin);

        roleUserRepository.save(new RoleUserEntity(new RoleUserId(user.getUserId(), role.getRoleId()), now, operatorId, null, null));

        auditRecorder.record(null, AuditAction.CREATE, "mt_user", user.getUserId(), trimmedAccount, null,
                             AuditRecorder.snapshot("帳號", trimmedAccount, "角色", roleName, "所屬租戶", tenant != null ? tenant.getTenantName() : null,
                                                    "信箱", user.getUserEmail()));
        log.info("Account created by {}: {} ({})", operatorId, trimmedAccount, roleName);
        return new TemporaryPasswordModel(toModel(user, tenant, roleName), temporaryPassword);
    }

    /**
     * 啟用或停用帳號。不能停用自己，避免總管理員把自己鎖在外面
     */
    @Transactional
    public AccountModel setEnabled(String userId, boolean enabled, String operatorId) {
        UserEntity user = findUser(userId);
        if (!enabled && userId.equals(operatorId)) {
            throw new ConflictException("不能停用自己的帳號");
        }

        user.setUserEnabled(enabled);
        touch(user, operatorId);
        if (!enabled) {
            redisRefreshTokenRepository.deleteAllByUsername(user.getUserAccount());
        }
        auditRecorder.record(null, enabled ? AuditAction.ENABLE : AuditAction.DISABLE, "mt_user", userId, user.getUserAccount(),
                             AuditRecorder.snapshot("啟用", !enabled), AuditRecorder.snapshot("啟用", enabled));
        log.info("Account {} by {}: {}", enabled ? "enabled" : "disabled", operatorId, user.getUserAccount());
        return toModel(user, findTenant(user.getTenantId()));
    }

    /**
     * 重設為新的臨時密碼：使用者下次登入必須修改密碼；該帳號已登入的裝置在 access token 到期後需要重新登入
     */
    @Transactional
    public TemporaryPasswordModel resetPassword(String userId, String operatorId) {
        UserEntity user = findUser(userId);
        SigninEntity signin = signinRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("找不到這個帳號"));

        String temporaryPassword = newTemporaryPassword(user.getUserAccount());
        long now = System.currentTimeMillis();
        signin.setSigninPw(passwordEncoder.encode(temporaryPassword));
        signin.setUpdateTime(now);
        signin.setUpdateUser(operatorId);
        user.setMustChangePassword(true);
        touch(user, operatorId);

        redisRefreshTokenRepository.deleteAllByUsername(signin.getSigninAccount());
        loginAttemptService.reset(signin.getSigninAccount());

        // 不記錄任何密碼內容
        auditRecorder.record(null, AuditAction.RESET_PASSWORD, "mt_user", userId, user.getUserAccount(),
                             AuditRecorder.snapshot("密碼", "（原密碼）"), AuditRecorder.snapshot("密碼", "（已重設為臨時密碼）"));
        log.info("Password reset by {}: {}", operatorId, user.getUserAccount());
        return new TemporaryPasswordModel(toModel(user, findTenant(user.getTenantId())), temporaryPassword);
    }

    private String newTemporaryPassword(String account) {
        String password = temporaryPasswordGenerator.generate();
        passwordPolicy.validate(account, password, null);
        return password;
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("找不到這個帳號"));
    }

    private TenantEntity findTenant(String tenantId) {
        return tenantId == null ? null : tenantRepository.findById(tenantId).orElse(null);
    }

    private void touch(UserEntity user, String operatorId) {
        user.setUpdateTime(System.currentTimeMillis());
        user.setUpdateUser(operatorId);
    }

    private AccountModel toModel(UserEntity user, TenantEntity tenant) {
        String roleName = roleRepository.findRolesByUserId(user.getUserId()).stream()
                .map(RoleEntity::getRoleName)
                .sorted()
                .findFirst()
                .orElse(null);
        return toModel(user, tenant, roleName);
    }

    private AccountModel toModel(UserEntity user, TenantEntity tenant, String roleName) {
        return new AccountModel(user.getUserId(), user.getUserAccount(), user.getUserEmail(), roleName, user.getTenantId(),
                                tenant != null ? tenant.getTenantName() : null, Boolean.TRUE.equals(user.getUserEnabled()),
                                Boolean.TRUE.equals(user.getMustChangePassword()), user.getCreateTime());
    }
}
