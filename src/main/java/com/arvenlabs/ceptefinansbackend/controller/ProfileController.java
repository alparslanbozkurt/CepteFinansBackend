package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.dto.request.ProfileUpdateRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.ProfileResponse;
import com.arvenlabs.ceptefinansbackend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile(), "Profil bilgileri başarıyla getirildi."));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateProfile(request), "Profil bilgileri başarıyla güncellendi."));
    }
}
