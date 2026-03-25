package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.entity.UserDeviceToken;
import com.arvenlabs.ceptefinansbackend.repository.UserDeviceTokenRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import com.arvenlabs.ceptefinansbackend.service.FCMService;
import com.arvenlabs.ceptefinansbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final FCMService fcmService;
    private final UserService userService;

    /**
     * POST /api/v1/users/fcm-token
     *
     * Cihaza ait FCM token'ını kaydeder veya günceller.
     * Her platform (ANDROID, IOS, WEB) için ayrı kayıt tutulur.
     * Token null/boş ise hiçbir şey yapılmaz (web loginlerinde mobil token korunur).
     *
     * Request Body: { "token": "<fcm_token>", "platform": "ANDROID" }
     */
    @PostMapping("/fcm-token")
    public ResponseEntity<String> updateFcmToken(@RequestBody Map<String, String> request) {
        String token    = request.get("token");
        String platform = request.getOrDefault("platform", "UNKNOWN").toUpperCase();

        // Boş token gelirse işlem yapma — web client'ın mobil token'ını silmesini önler
        if (token == null || token.isBlank()) {
            return ResponseEntity.ok("Token boş, güncelleme yapılmadı.");
        }

        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Aynı platform için kayıt varsa token'ı güncelle (upsert), yoksa yeni oluştur
        UserDeviceToken deviceToken = deviceTokenRepository.findByUserAndPlatform(user, platform)
                .map(existing -> {
                    existing.setFcmToken(token);
                    return existing;
                })
                .orElse(UserDeviceToken.builder()
                        .user(user)
                        .fcmToken(token)
                        .platform(platform)
                        .build());

        deviceTokenRepository.save(deviceToken);
        return ResponseEntity.ok("Cihaz token başarıyla kaydedildi. Platform: " + platform);
    }

    /**
     * POST /api/v1/users/test-push
     *
     * Kullanıcının kayıtlı tüm cihazlarına test bildirimi gönderir.
     */
    @PostMapping("/test-push")
    public ResponseEntity<String> testPushNotification(@RequestBody Map<String, String> request) {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        List<UserDeviceToken> devices = deviceTokenRepository.findAllByUser(user);
        if (devices.isEmpty()) {
            return ResponseEntity.badRequest().body("Kullanıcının kayıtlı cihazı yok. Lütfen önce /fcm-token ile kaydedin.");
        }

        for (UserDeviceToken device : devices) {
            try {
                fcmService.sendPushNotification(device.getFcmToken(), request.get("title"), request.get("message"));
            } catch (Exception e) {
                // Tek cihaz hatası diğerlerini durdurmasın
            }
        }
        return ResponseEntity.ok("Firebase'e " + devices.size() + " cihaza mesaj gönderme emri verildi!");
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        userService.deleteCurrentUserAndAllData(email);
        return ResponseEntity.ok("Hesabınız ve tüm verileriniz kalıcı olarak silinmiştir.");
    }
}