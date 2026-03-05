package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.model.entity.PasswordResetToken;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.PasswordResetTokenRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void processForgotPassword(String email) {
        // 1. Veritabanından e-posta adresine göre kullanıcıyı bul
        Optional<User> userOptional = userRepository.findByEmail(email);

        // 2. Güvenlik Önlemi (User Enumeration Koruması): Kullanıcı yoksa işlem yapma ama hata da fırlatma
        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();

        // 3. Benzersiz bir token string'i oluştur
        String token = UUID.randomUUID().toString();

        // 4. Token entity'sini oluştur ve süresini belirle (15 dakika)
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        // 5. Token'ı veritabanına kaydet
        tokenRepository.save(resetToken);

        // 6. YENİ: Link oluşturma işini ve e-posta şablonunu EmailService'e devrettik!
        // application.properties'deki frontend URL'sini (localhost:3000) kullanarak maili asenkron atacak.
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 1. Token'ı veritabanında bul
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Geçersiz veya bulunamayan token."));

        // 2. Token'ın süresi dolmuş mu kontrol et
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken); // Süresi dolmuşsa temizle
            throw new RuntimeException("Bu şifre sıfırlama bağlantısının süresi dolmuş.");
        }

        // 3. Kullanıcıyı al ve şifresini güvenli bir şekilde (BCrypt ile) güncelle
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 4. Güvenlik için kullanılmış token'ı sil (Aynı linkle tekrar şifre değiştirilemesin)
        tokenRepository.delete(resetToken);
    }
}