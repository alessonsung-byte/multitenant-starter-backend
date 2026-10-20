package com.example.starter.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <code>PingController</code> 確認服務是否正常運作用，不需要登入
 */
@RestController
@RequestMapping("/ping")
public class PingController {

    @GetMapping
    public String ping() {
        return "pong";
    }
}
