package com.example.starter.log.advice;

import com.example.starter.log.annotation.LogRequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * <code>RequestBodyLoggingHandler</code> 紀錄 request body 內容。
 * <br>
 * 預設<b>不記錄</b>；只有 controller class 或 method 標記 {@link LogRequestBody} 時才會記錄。
 * <br>
 * 之所以寫在這裡而不是 <code>LoggingInterceptor</code>：request body 只能讀一次，
 * 在 interceptor 讀取後 controller 就拿不到了。
 *
 * @see LogRequestBody
 * @see com.example.starter.log.config.LoggingInterceptor
 */
@Slf4j
@ControllerAdvice
public class RequestBodyLoggingHandler extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(@NonNull MethodParameter methodParameter, @NonNull Type targetType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.getContainingClass().isAnnotationPresent(LogRequestBody.class)
                || Objects.requireNonNull(methodParameter.getMethod()).isAnnotationPresent(LogRequestBody.class);
    }

    @Override
    public @NonNull Object afterBodyRead(@NonNull Object body, @NonNull HttpInputMessage inputMessage, @NonNull MethodParameter parameter, @NonNull Type targetType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        log.info("Request body: {}", body);
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
