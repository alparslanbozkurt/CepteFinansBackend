package com.arvenlabs.ceptefinansbackend.controller.ai;

import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.ai.AiRoastResponse;
import com.arvenlabs.ceptefinansbackend.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/roast")
    public ResponseEntity<ApiResponse<AiRoastResponse>> roastMe() {
        return ResponseEntity.ok(ApiResponse.success(aiService.generateRoast(), "AI finansal hezimeti başarıyla oluşturuldu."));
    }
}
