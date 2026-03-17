package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import com.arvenlabs.ceptefinansbackend.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final FCMService fcmService;

    @PostMapping("/fcm-token")
    public ResponseEntity<String> updateFcmToken(@RequestBody Map<String, String> request) {
        // 1. İsteği atan kullanıcıyı Security Context'ten bul (JWT sayesinde)
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();

        // 2. Kullanıcıyı veritabanından çek
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // 3. Yeni token'ı kaydet
        String token = request.get("token");
        user.setFcmToken(token);
        userRepository.save(user);

        return ResponseEntity.ok("Cihaz token başarıyla güncellendi.");
    }

    @PostMapping("/test-push")
    public ResponseEntity<String> testPushNotification(@RequestBody Map<String, String> request) {
        // İsteği atan kullanıcıyı bul
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Kullanıcının kayıtlı bir cihaz token'ı var mı?
        if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Kullanıcının FCM Token'ı yok. Lütfen önce token kaydedin.");
        }

        // NotificationService veya FCMService üzerinden Firebase'e isteği yolla
        try {
            // İhtiyacın olan servisi UserController içine enjekte etmeyi unutma (private final FCMService fcmService;)
            fcmService.sendPushNotification(
                    user.getFcmToken(),
                    request.get("title"),
                    request.get("message")
            );
            return ResponseEntity.ok("Firebase'e mesaj gönderme emri verildi!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }
}