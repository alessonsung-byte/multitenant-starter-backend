package com.example.starter.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * <code>AppUserDetailsService</code> 讓 Spring Security 從資料庫讀取使用者、角色與權限
 */
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserHelper userHelper;

    /**
     * @param username 登入帳號，即 mt_signin.signin_account
     * @throws UsernameNotFoundException 找不到此帳號時丟出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userHelper.findUserDetailsBySigninAccount(username);
    }
}
