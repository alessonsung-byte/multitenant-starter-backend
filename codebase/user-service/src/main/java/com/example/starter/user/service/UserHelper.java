package com.example.starter.user.service;

import com.example.starter.common.service.MessageHelper;
import com.example.starter.user.entity.TenantEntity;
import com.example.starter.user.entity.PermissionEntity;
import com.example.starter.user.entity.RoleEntity;
import com.example.starter.user.entity.SigninEntity;
import com.example.starter.user.entity.UserEntity;
import com.example.starter.user.model.AppUserDetails;
import com.example.starter.user.repository.TenantRepository;
import com.example.starter.user.repository.PermissionRepository;
import com.example.starter.user.repository.RoleRepository;
import com.example.starter.user.repository.SigninRepository;
import com.example.starter.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <code>UserHelper</code> 依登入帳號查詢使用者、角色與權限，組成 Spring Security 使用的 {@link AppUserDetails}
 */
@Component
@RequiredArgsConstructor
public class UserHelper {

    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final SigninRepository signinRepository;

    private final TenantRepository tenantRepository;

    private final MessageHelper messageHelper;

    /**
     * 依登入帳號取得完整的使用者資訊（含角色與權限）
     *
     * @param signinAccount 登入帳號，即 mt_signin.signin_account，也就是登入後 {@code Authentication.getName()} 的值
     * @throws UsernameNotFoundException 找不到此帳號時丟出
     */
    @Transactional(readOnly = true)
    public AppUserDetails findUserDetailsBySigninAccount(String signinAccount) throws UsernameNotFoundException {
        SigninEntity signinEntity = signinRepository.findBySigninAccount(signinAccount)
                .orElseThrow(() -> userNotFound(signinAccount));

        UserEntity userEntity = userRepository.findById(signinEntity.getUserId())
                .orElseThrow(() -> userNotFound(signinAccount));

        Set<RoleEntity> roles = roleRepository.findRolesByUserId(userEntity.getUserId());

        TenantEntity tenantEntity = userEntity.getTenantId() == null ? null
                : tenantRepository.findById(userEntity.getTenantId()).orElse(null);

        return new AppUserDetails(userEntity, signinEntity, tenantEntity, roleNamesFromRoles(roles), permissionNamesFromRoles(roles));
    }

    private Set<String> roleNamesFromRoles(Collection<RoleEntity> roles) {
        return roles.stream()
                .map(RoleEntity::getRoleName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private Set<String> permissionNamesFromRoles(Collection<RoleEntity> roles) {
        return roles.stream()
                .map(RoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .map(permissionRepository::findPermissionsByRoleId)
                .flatMap(Collection::stream)
                .map(PermissionEntity::getPermissionName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private UsernameNotFoundException userNotFound(String signinAccount) {
        return new UsernameNotFoundException(messageHelper.localizedMessage("user.not.found.by.name", new String[]{signinAccount}));
    }
}
