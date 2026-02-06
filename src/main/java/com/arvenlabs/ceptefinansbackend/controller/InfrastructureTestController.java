package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/infra-test")
@Slf4j // Logger
public class InfrastructureTestController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        log.info("Ping isteği geldi. Sistem ayakta.");
        return ApiResponse.success("Pong!", "Altyapı sorunsuz çalışıyor.");
    }

    @GetMapping("/error-test")
    public ApiResponse<String> triggerError() {
        // Bu hata GlobalExceptionHandler tarafından yakalanıp JSON'a çevrilmeli
        throw new RuntimeException("Test amaçlı fırlatılan hata!");
    }
}