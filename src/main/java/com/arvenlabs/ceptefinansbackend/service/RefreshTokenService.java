package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.model.entity.RefreshToken;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.RefreshTokenRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // 1. Refresh Token Oluştur (7 Günlük)
    public RefreshToken createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString()) // Rastgele, tahmin edilemez bir string
                .expiresAt(LocalDateTime.now().plusDays(7)) // 7 Gün geçerli
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // 2. Token'ı Veritabanında Bul
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh Token veritabanında bulunamadı!"));
    }

    // 3. Süresi Dolmuş mu Kontrol Et
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token); // Süresi dolmuşsa sil
            throw new RuntimeException("Refresh Token süresi dolmuş. Lütfen tekrar giriş yapın.");
        }
        return token;
    }

    // 4. Kullanıcıya Ait Tüm Tokenları Sil (Logout için)
    public void deleteByUserId(UUID userId) {
        // Kullanıcıyı bulup, repo'daki deleteByUser metodunu çağıracağız.
        // Ancak JPA delete işlemleri Transactional olmalı, bunu Service katmanında yönetmek daha iyi.
        // Basitlik için burada repo metodunu kullanacağız ama Repository'e @Transactional eklemeliyiz.
        // Şimdilik repo koduna dokunmadan, kullanıcı üzerinden gidelim:
        User user = userRepository.findById(userId).orElseThrow();
        refreshTokenRepository.deleteByUser(user);
    }

    // Logout işlemi için token silme
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}