package com.example.starter.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.starter.common.model.resp.RespErrorModel;
import com.example.starter.common.model.resp.RespMessageCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <code>CustomAuthenticationEntryPoint</code> 呼叫需要登入的 API，但沒有帶 access token、或 token 無效／過期時，回傳 401
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RespErrorModel respErrorModel = new RespErrorModel(RespMessageCode.ERROR_UNAUTHORIZED, "尚未登入或登入已逾時");
        response.getWriter().write(objectMapper.writeValueAsString(respErrorModel));
    }
}
