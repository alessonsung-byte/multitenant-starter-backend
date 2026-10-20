package com.example.starter.user.config;

import com.example.starter.user.model.AppPrincipal;
import com.example.starter.user.model.AppUserDetails;
import com.example.starter.user.service.JwtHelper;
import com.example.starter.user.tenant.TenantContext;
import com.example.starter.user.tenant.TenantState;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <code>JwtAuthenticationFilter</code> 在每次請求時檢查 HTTP header {@code Authorization: Bearer <access token>}。
 * <br>
 * token 有效且帳號仍啟用時，把登入者資訊放進 Spring Security；token 缺少、無效或過期時<b>不丟出例外</b>，
 * 直接以「未登入」身分繼續，交由 Spring Security 對需要登入的 API 回傳統一格式的 401
 * （前端收到 401 會自動用 refresh token 換發新的 access token）。
 * <br>
 * 每次請求都重新讀取帳號與所屬租戶：帳號或租戶被停用時，下一次請求立即視同未登入（401）。
 * 同時設定這個請求的「目前租戶」（{@link TenantContext}），請求結束時一定清除。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtHelper jwtHelper;

    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtHelper jwtHelper, UserDetailsService userDetailsService) {
        this.jwtHelper = jwtHelper;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (StringUtils.startsWith(authorizationHeader, BEARER_PREFIX)) {
                authenticate(request, authorizationHeader.substring(BEARER_PREFIX.length()));
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request, String accessToken) {
        String username;
        try {
            username = jwtHelper.parseAccessToken(accessToken).getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            log.info("Invalid access token from {}: {}", request.getRemoteAddr(), ex.getMessage());
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            log.info("Access token refers to a user that no longer exists: {}", username);
            return;
        }

        if (!userDetails.isEnabled()) {
            log.info("Access token refers to a disabled user or a user of a disabled tenant: {}", username);
            return;
        }

        AppPrincipal principal = AppPrincipal.from((AppUserDetails) userDetails);
        TenantContext.set(tenantStateOf(principal));

        // 不放入密碼。之後 Authentication.getName() 取得的是登入帳號
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /**
     * 總管理員可以讀取所有租戶；租戶帳號只能存取自己的租戶；兩者都不是的帳號（資料異常）看不到任何租戶的資料
     */
    private TenantState tenantStateOf(AppPrincipal principal) {
        if (principal.sysAdmin()) {
            return TenantState.allTenants(principal.userId());
        }
        if (principal.tenantId() != null) {
            return TenantState.tenant(principal.userId(), principal.tenantId());
        }
        return new TenantState(principal.userId(), null, false);
    }
}
