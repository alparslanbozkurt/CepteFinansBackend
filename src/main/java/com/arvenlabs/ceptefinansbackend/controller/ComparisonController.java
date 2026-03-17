package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.dto.request.UserUpdateRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import com.arvenlabs.ceptefinansbackend.service.ComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comparison")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final UserRepository userRepository;

    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<String>> getInsights() {
        return ResponseEntity
                .ok(ApiResponse.success(comparisonService.getComparisonInsights(), "İçgörüler başarıyla getirildi"));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@RequestBody UserUpdateRequest request) {
        User user = getCurrentUser();
        user.setAge(request.getAge());
        user.setIncome(request.getIncome());
        user.setOccupation(request.getOccupation());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Profil bilgileriniz güncellendi"));
    }

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}
