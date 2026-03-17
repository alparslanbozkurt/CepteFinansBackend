package com.arvenlabs.ceptefinansbackend.controller.analytics;

import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.analytics.OrbitResponse;
import com.arvenlabs.ceptefinansbackend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/orbit")
    public ResponseEntity<ApiResponse<List<OrbitResponse>>> getWealthOrbit() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getWealthOrbit(), "Wealth Orbit verileri başarıyla getirildi."));
    }
}
