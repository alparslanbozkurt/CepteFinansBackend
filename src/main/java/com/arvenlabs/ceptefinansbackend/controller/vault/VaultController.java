package com.arvenlabs.ceptefinansbackend.controller.vault;

import com.arvenlabs.ceptefinansbackend.dto.request.VaultRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.VaultResponse;
import com.arvenlabs.ceptefinansbackend.service.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @PostMapping
    public ResponseEntity<ApiResponse<VaultResponse>> createLock(@RequestBody VaultRequest request) {
        VaultResponse response = vaultService.lockCategory(request);
        return ResponseEntity.ok(ApiResponse.success(response, "48 Saatlik harcamama yemin kasanız başarıyla oluşturuldu."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VaultResponse>>> getLocks() {
        return ResponseEntity.ok(ApiResponse.success(vaultService.getMyLocks(), "Harcama kilitleri başarıyla getirildi."));
    }
}
