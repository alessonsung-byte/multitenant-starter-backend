package com.example.starter;

import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ApiServerApplication {

    public static void main(String[] args) {
        /*
           使用 StandardEncryptableEnvironment 取代 Spring Boot 預設的 Environment，
           讓設定檔中以 ENC(...) 加密的值在啟動一開始就能解密（見 doc/HOW_TO_ENCRYPT_STRING_IN_PROPERTIES.md）
         */
        new SpringApplicationBuilder().environment(new StandardEncryptableEnvironment()).sources(ApiServerApplication.class).run(args);
    }

}
