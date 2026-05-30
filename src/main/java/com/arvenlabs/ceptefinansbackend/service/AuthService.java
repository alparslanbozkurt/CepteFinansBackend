package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.LoginRequest;
import com.arvenlabs.ceptefinansbackend.dto.request.RegisterRequest;
import com.arvenlabs.ceptefinansbackend.dto.request.VerifyEmailRequest; // Bunu birazdan oluşturacağız
import com.arvenlabs.ceptefinansbackend.dto.response.AuthResponse;
import com.arvenlabs.ceptefinansbackend.exception.BadRequestException;
import com.arvenlabs.ceptefinansbackend.model.entity.RefreshToken;
import com.arvenlabs.ceptefinansbackend.model.entity.SecurityCode;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.SecurityCodeType;
import com.arvenlabs.ceptefinansbackend.repository.SecurityCodeRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    // YENİ EKLENEN BAĞIMLILIKLAR
    private final SecurityCodeRepository securityCodeRepository;
    private final EmailService emailService;

    // --- 1. KAYIT OL (Token dönmez, Mail atar) ---
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.isTermsAccepted()) {
            throw new RuntimeException("Kayıt olabilmek için Kullanım Koşullarını onaylamanız gerekmektedir.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda!");
        }

        // 1. Kullanıcıyı oluştur (isEmailVerified varsayılan olarak false kalmalı)
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .is2faEnabled(false)
                .isTermsAccepted(true)
                .isEmailVerified(false)
                .build();

        userRepository.save(user);

        // 2. 6 Haneli Rastgele Güvenlik Kodu Üret
        String generatedCode = generateSixDigitCode();

        // 3. Kodu Veritabanına Kaydet
        SecurityCode securityCode = SecurityCode.builder()
                .user(user)
                .code(generatedCode)
                .type(SecurityCodeType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // 5 Dakika Geçerli
                .isUsed(false)
                .build();

        securityCodeRepository.save(securityCode);

        // 4. Asenkron olarak maili gönder (Kullanıcıyı burada bekletmiyoruz!)
        emailService.sendVerificationEmail(user.getEmail(), generatedCode);

        // 5. Sadece mesaj dön (Token YOK!)
        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .message("Kayıt başarılı. Lütfen e-postanıza gönderilen 6 haneli kodu girin.")
                .build();
    }

    // --- 2. E-POSTA DOĞRULAMA (Yeni Metot - Kodu Girip Token Alır) ---
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        // Kullanıcıyı bul
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Kullanıcı bulunamadı."));

        if (user.isEmailVerified()) {
            throw new BadRequestException("E-posta adresiniz zaten onaylanmış. Giriş yapabilirsiniz.");
        }

        // Kullanıcının en son üretilen, kullanılmamış onay kodunu getir (Senin yazdığın
        SecurityCode securityCode = securityCodeRepository
                .findFirstByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(user, SecurityCodeType.EMAIL_VERIFICATION)
                .orElseThrow(
                        () -> new BadRequestException("Geçerli bir doğrulama kodu bulunamadı. Lütfen yeni kod isteyin."));

        // Kod doğru mu?
        if (!securityCode.getCode().equals(request.getCode())) {
            throw new BadRequestException("Doğrulama kodu hatalı, lütfen tekrar deneyin.");
        }

        // Kodun süresi dolmuş mu?
        if (securityCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Bu kodun süresi dolmuş (5 dakika). Lütfen yeni bir kod isteyin.");
        }

        // HER ŞEY BAŞARILI: Kodu kullanıldı olarak işaretle
        securityCode.setUsed(true);
        securityCodeRepository.save(securityCode);

        // Kullanıcının onayını ver
        user.setEmailVerified(true);
        userRepository.save(user);

        // KULLANICIYI DOĞRUDAN İÇERİ ALIYORUZ (Token'ları şimdi veriyoruz)
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .message("E-posta doğrulandı! Hoş geldiniz.")
                .build();
    }

    // --- 3. GİRİŞ YAP (Onay Kontrolü Eklendi) ---
    public AuthResponse login(LoginRequest request) {
        // Önce kullanıcıyı bul ki onay durumuna bakalım
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        // YENİ: E-postası onaylanmamışsa girişi engelle!
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Lütfen giriş yapmadan önce e-posta adresinizi onaylayın.");
        }

        // Şifre kontrolü
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .message("Giriş başarılı.")
                .build();
    }

    // --- ACCESS TOKEN YENİLEME ---
    @Transactional
    public AuthResponse refreshToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenService.findByToken(requestRefreshToken);
        refreshTokenService.verifyExpiration(token);
        User user = token.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(requestRefreshToken)
                .message("Token yenilendi.")
                .build();
    }

    // --- LOGOUT ---
    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }

    // --- YARDIMCI METOT: 6 Haneli Kod Üretici ---
    private String generateSixDigitCode() {
        Random random = new Random();
        int number = random.nextInt(999999);
        return String.format("%06d", number); // 001234 gibi sıfırla başlayanları da korur
    }
}