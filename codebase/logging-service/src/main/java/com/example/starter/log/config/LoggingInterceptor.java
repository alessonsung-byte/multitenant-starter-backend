package com.example.starter.log.config;

import com.example.starter.log.annotation.NoLogToHeader;
import com.example.starter.log.annotation.NoLogToHeaderAndParam;
import com.example.starter.log.annotation.NoLogToParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <code>LoggingInterceptor</code> 每個呼叫 controller 的請求，在開始與結束時各記錄一行日誌，
 * 兩行以同一個 requestId 對應。
 * <br>
 * 開始時記錄 method、網址、query string、headers、parameters；controller class 或 method 標記下列註解時不記錄對應內容：
 * <ul>
 *     <li>{@link NoLogToHeaderAndParam}：不記錄 headers 與 parameters</li>
 *     <li>{@link NoLogToHeader}：不記錄 headers</li>
 *     <li>{@link NoLogToParam}：不記錄 parameters</li>
 * </ul>
 * 不論有沒有標記，{@link #MASKED_HEADERS} 這些會帶登入憑證的 header 一律只記錄 "***"。
 * <br>
 * request body 不在這裡記錄（讀取後 controller 就拿不到了），見 {@link com.example.starter.log.advice.RequestBodyLoggingHandler}。
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    /**
     * 帶有 access token、cookie 等登入憑證的 header（小寫），日誌中一律遮蔽，避免 token 外洩到日誌檔
     */
    private static final Set<String> MASKED_HEADERS = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie");

    private static final String REQUEST_ID_ATTRIBUTE = LoggingInterceptor.class.getName() + ".requestId";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);

        StringBuilder builder = new StringBuilder("Incoming request: requestId=").append(requestId)
                .append(", method=").append(request.getMethod())
                .append(", uri=").append(request.getRequestURI());
        if (request.getQueryString() != null) {
            builder.append(", query={").append(request.getQueryString()).append("}");
        }
        if (request.getContentType() != null) {
            builder.append(", requestContentType=").append(request.getContentType());
        }
        builder.append(", requestContentLength=").append(request.getContentLength());

        if (!hasAnnotation(handlerMethod, NoLogToHeaderAndParam.class)) {
            if (!hasAnnotation(handlerMethod, NoLogToHeader.class)) {
                appendHeaders(builder, request);
            }
            if (!hasAnnotation(handlerMethod, NoLogToParam.class)) {
                appendParameters(builder, request);
            }
        }

        log.info(builder.toString());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {
        // 與 preHandle 一致：只記錄對應到 controller 的請求
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        StringBuilder builder = new StringBuilder("Request completed: requestId=").append(request.getAttribute(REQUEST_ID_ATTRIBUTE))
                .append(", status=").append(response.getStatus());
        if (response.getContentType() != null) {
            builder.append(", responseContentType=").append(response.getContentType());
        }

        // controller 丟出的例外一般已由 GlobalExceptionHandler 處理（此時 ex 為 null），這裡只會收到沒有被處理的例外
        if (ex != null) {
            log.error(builder.toString(), ex);
        } else {
            log.info(builder.toString());
        }
    }

    private static void appendHeaders(StringBuilder builder, HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        for (String headerName : Collections.list(request.getHeaderNames())) {
            String headerValue = MASKED_HEADERS.contains(headerName.toLowerCase(Locale.ROOT)) ? "***" : request.getHeader(headerName);
            headers.append(headerName).append("=").append(headerValue).append(",");
        }
        if (!headers.isEmpty()) {
            builder.append(", headers={").append(headers).append("}");
        }
    }

    private static void appendParameters(StringBuilder builder, HttpServletRequest request) {
        StringBuilder parameters = new StringBuilder();
        for (String paramName : request.getParameterMap().keySet()) {
            parameters.append(paramName).append("=").append(request.getParameter(paramName)).append(",");
        }
        if (!parameters.isEmpty()) {
            builder.append(", parameters={").append(parameters).append("}");
        }
    }

    private static boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends java.lang.annotation.Annotation> annotationType) {
        return handlerMethod.hasMethodAnnotation(annotationType) || handlerMethod.getBeanType().isAnnotationPresent(annotationType);
    }
}
