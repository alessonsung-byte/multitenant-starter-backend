package com.example.starter.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.starter.common.model.resp.RespErrorModel;
import com.example.starter.common.model.resp.RespMessageCode;
import com.example.starter.user.model.AppPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <code>PasswordChangeRequiredFilter</code> 強制修改密碼。
 * <br>
 * 帳號標記為「需要修改密碼」時（新帳號第一次登入、被總管理員重設密碼後），
 * 除了「修改密碼」（{@code POST /api/me/password}）以外的 API 一律回 403（{@code ERROR_PASSWORD_CHANGE_REQUIRED}）；
 * 登入、登出等 {@code /api/public/**} 不需要登入的 API 不受影響。前端收到這個錯誤時導向修改密碼頁。
 * <br>
 * 放在 {@code JwtAuthenticationFilter} 之後執行（見 {@link ProjectConfig}），此時登入者資訊已經是最新的資料庫內容。
 */
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    static final String CHANGE_PASSWORD_PATH = "/api/me/password";

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean mustChangePassword = authentication != null
                && authentication.getPrincipal() instanceof AppPrincipal principal
                && principal.mustChangePassword();

        if (mustChangePassword && !isChangePasswordRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            RespErrorModel error = new RespErrorModel(RespMessageCode.ERROR_PASSWORD_CHANGE_REQUIRED, "請先修改密碼");
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isChangePasswordRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return HttpMethod.POST.matches(request.getMethod()) && CHANGE_PASSWORD_PATH.equals(path);
    }
}
