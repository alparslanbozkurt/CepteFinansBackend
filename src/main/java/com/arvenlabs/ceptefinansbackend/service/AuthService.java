package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.LoginRequest;
import com.arvenlabs.ceptefinansbackend.dto.request.RegisterRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.AuthResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.RefreshToken;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService; // YENİ EKLENDİ

    // --- KAYIT OL ---
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda!");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .is2faEnabled(false)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail()); // YENİ

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken()) // YENİ
                .message("Kayıt işlemi başarılı.")
                .build();
    }

    // --- GİRİŞ YAP ---
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail()); // YENİ

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken()) // YENİ
                .message("Giriş başarılı.")
                .build();
    }

    // --- ACCESS TOKEN YENİLEME (REFRESH TOKEN FLOW) ---
    public AuthResponse refreshToken(String requestRefreshToken) {
        // 1. Veritabanından tokenı bul
        RefreshToken token = refreshTokenService.findByToken(requestRefreshToken);

        // 2. Süresini kontrol et
        refreshTokenService.verifyExpiration(token);

        // 3. Token hala geçerliyse, kime ait olduğunu bul
        User user = token.getUser();

        // 4. Yeni bir Access Token üret
        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(requestRefreshToken) // Refresh token değişmedi, aynısını dönüyoruz
                .message("Token yenilendi.")
                .build();
    }

    // --- LOGOUT (ÇIKIŞ YAP) ---
    public void logout(String refreshToken) {
        // Token'ı veritabanından siliyoruz.
        // Böylece saldırgan bu token'ı çalsa bile artık geçersiz olur.
        refreshTokenService.deleteByToken(refreshToken);
    }
}