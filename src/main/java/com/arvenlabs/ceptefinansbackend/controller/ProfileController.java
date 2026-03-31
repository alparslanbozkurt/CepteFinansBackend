package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.dto.request.ProfileUpdateRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.ProfileResponse;
import com.arvenlabs.ceptefinansbackend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import com.arvenlabs.ceptefinansbackend.model.entity.User;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile(), "Profil bilgileri başarıyla getirildi."));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateProfile(request), "Profil bilgileri başarıyla güncellendi."));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadProfileImage(@RequestParam("file") MultipartFile file) throws IOException {
        profileService.uploadProfileImage(file);
        return ResponseEntity.ok(ApiResponse.success(null, "Profil fotoğrafı başarıyla yüklendi."));
    }

    @GetMapping("/image/{userId}")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        if (user.getProfileImage() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, user.getProfileImageContentType())
                .body(user.getProfileImage());
    }
}
