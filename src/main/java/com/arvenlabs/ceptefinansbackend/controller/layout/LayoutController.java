package com.arvenlabs.ceptefinansbackend.controller.layout;

import com.arvenlabs.ceptefinansbackend.dto.request.LayoutRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.LayoutResponse;
import com.arvenlabs.ceptefinansbackend.service.LayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/layout")
@RequiredArgsConstructor
public class LayoutController {

    private final LayoutService layoutService;

    @PostMapping
    public ResponseEntity<ApiResponse<LayoutResponse>> saveLayout(@RequestBody LayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(layoutService.saveLayout(request), "Kullanıcı arayüz yerleşimi başarıyla kaydedildi."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LayoutResponse>> getLayout() {
        return ResponseEntity.ok(ApiResponse.success(layoutService.getLayout(), "Kullanıcı arayüz yerleşimi getirildi."));
    }
}
