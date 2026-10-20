package com.example.starter.user.model;

import com.example.starter.user.entity.TenantEntity;
import com.example.starter.user.entity.SigninEntity;
import com.example.starter.user.entity.UserEntity;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * <code>AppUserDetails</code> 登入者的資訊（帳號、密碼雜湊、角色、權限），提供給 Spring Security 使用。
 * <br>
 * 不使用 Spring Security 內建的 {@code User.withUsername(...).authorities(...).roles(...)}：
 * 該寫法中 {@code roles()} 會覆蓋先前 {@code authorities()} 設定的權限，導致 {@code @PreAuthorize("hasAuthority(...)")} 判斷失敗。
 * 這裡自行把權限與角色（加上 {@code ROLE_} 前綴）合併成同一組 authorities。
 */
@Getter
public class AppUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 4537734423526938425L;

    /**
     * 總管理員的角色名稱
     */
    public static final String ROLE_SYS_ADMIN = "SYS_ADMIN";

    /**
     * 租戶帳號的角色名稱
     */
    public static final String ROLE_TENANT_USER = "TENANT_USER";

    private final UserEntity user;

    private final SigninEntity signin;

    /**
     * 帳號所屬的租戶；總管理員為 null
     */
    private final TenantEntity tenant;

    /**
     * 使用者所有角色的名稱（{@link com.example.starter.user.entity.RoleEntity#getRoleName()}）
     */
    private final Set<String> roles;

    /**
     * 使用者所有角色擁有的權限名稱（{@link com.example.starter.user.entity.PermissionEntity#getPermissionName()}），已去除重複
     */
    private final Set<String> permissions;

    /**
     * permissions 與 roles 轉換後的 {@link SimpleGrantedAuthority}；角色名稱會加上前綴 {@code ROLE_}
     */
    private final Set<GrantedAuthority> grantedAuthorities;

    public AppUserDetails(UserEntity user, SigninEntity signin, TenantEntity tenant, Set<String> roles, Set<String> permissions) {
        this.user = user;
        this.signin = signin;
        this.tenant = tenant;
        this.roles = roles;
        this.permissions = permissions;

        grantedAuthorities = grantedAuthoritiesFromRolesAndPermissions(roles, permissions);
    }

    /**
     * 把權限名稱與角色名稱轉換為 {@link SimpleGrantedAuthority}。
     * 角色名稱會加上前綴 {@code ROLE_}；角色名稱本身若已經以 {@code ROLE_} 開頭會丟出例外
     */
    private Set<GrantedAuthority> grantedAuthoritiesFromRolesAndPermissions(Set<String> roleNames, Set<String> permissionNames) {
        final Set<GrantedAuthority> authorities = new HashSet<>();

        if (permissionNames != null) {
            permissionNames.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }

        if (roleNames != null) {
            for (String role : roleNames) {
                Assert.isTrue(!role.startsWith("ROLE_"),
                              () -> String.format("角色名稱 `%s` 不合法：角色名稱不能以 `ROLE_` 開頭", role));

                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        return authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    /**
     * 回傳使用者登入密碼。即 {@link SigninEntity#getSigninPw()}
     */
    @Override
    public String getPassword() {
        return Optional.ofNullable(signin).map(SigninEntity::getSigninPw).orElse(null);
    }

    /**
     * 即 {@link Authentication#getName()} 取回的值。
     * 此值是使用者登入帳號({@link SigninEntity#getSigninAccount()})，
     * 而不是使用者編號({@link UserEntity#getUserId()})。
     */
    @Override
    public String getUsername() {
        return Optional.ofNullable(signin).map(SigninEntity::getSigninAccount).orElse(null);
    }

    /**
     * 帳號可以使用的條件：帳號本身啟用，而且所屬租戶（如果有）也啟用。
     * <br>
     * 每次請求都會重新讀取，所以停用帳號或停用租戶後，下一次請求立即生效
     */
    @Override
    public boolean isEnabled() {
        boolean userEnabled = Optional.ofNullable(user).map(UserEntity::getUserEnabled).orElse(false);
        boolean tenantEnabled = tenant == null || Boolean.TRUE.equals(tenant.getTenantEnabled());
        boolean tenantMissing = tenant == null && user != null && user.getTenantId() != null;
        return userEnabled && tenantEnabled && !tenantMissing;
    }

    public boolean isSysAdmin() {
        return roles != null && roles.contains(ROLE_SYS_ADMIN);
    }

    /**
     * 所屬租戶的編號；總管理員為 null
     */
    public String getTenantId() {
        return user != null ? user.getTenantId() : null;
    }

    public boolean isMustChangePassword() {
        return user != null && Boolean.TRUE.equals(user.getMustChangePassword());
    }

    @Override
    public boolean isAccountNonExpired() {
        return isEnabled();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isEnabled();
    }
}
