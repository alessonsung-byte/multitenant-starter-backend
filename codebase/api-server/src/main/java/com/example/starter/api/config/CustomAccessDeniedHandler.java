package com.example.starter.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.starter.common.model.resp.RespErrorModel;
import com.example.starter.common.model.resp.RespMessageCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <code>CustomAccessDeniedHandler</code> 已登入，但沒有呼叫此 API 所需的角色或權限時，回傳 403
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        log.info("Access denied: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RespErrorModel respErrorModel = new RespErrorModel(RespMessageCode.ERROR_AUTHORIZATION_DENIED, "沒有執行此功能的權限");
        response.getWriter().write(objectMapper.writeValueAsString(respErrorModel));
    }
}
