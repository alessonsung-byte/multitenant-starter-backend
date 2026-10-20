package com.example.starter.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.starter.common.model.resp.RespErrorModel;
import com.example.starter.common.model.resp.RespMessageCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <code>JsonRequestRejectedHandler</code> 網址被 Spring Security 的防火牆拒絕時（例如含 {@code %25}、{@code ..}、{@code ;}），回傳 400。
 * <br>
 * 預設的處理會丟出例外、轉到 {@code /error}，而 {@code /error} 需要登入，結果變成 401「尚未登入」，
 * 前端收到 401 會把使用者登出。這裡直接寫出 400，不經過 {@code /error}。
 */
@Component
@RequiredArgsConstructor
public class JsonRequestRejectedHandler implements RequestRejectedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       RequestRejectedException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RespErrorModel respErrorModel = new RespErrorModel(RespMessageCode.ERROR_INVALID_ARGUMENT, "網址格式不正確");
        response.getWriter().write(objectMapper.writeValueAsString(respErrorModel));
    }
}
