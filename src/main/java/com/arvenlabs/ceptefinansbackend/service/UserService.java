package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
// ... diğer importlar ...

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final NotificationRepository notificationRepository;
    private final com.arvenlabs.ceptefinansbackend.repository.ImpulseLockRepository impulseLockRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityCodeRepository securityCodeRepository;
    private final GoalRepository goalRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserLayoutRepository userLayoutRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    @Transactional
    public void deleteCurrentUserAndAllData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        UUID userId = user.getId();

        // 1. Kullanıcıya bağlı tüm verileri manuel olarak sil (Sıra önemli değil ama user'dan önce silinmeliler)
        transactionRepository.deleteAllByUserId(userId); // İşlemler gitti
        budgetRepository.deleteAllByUserId(userId); // Bütçeler gitti
        notificationRepository.deleteAllByUserId(userId); // Bildirimler gitti
        impulseLockRepository.deleteAllByUserId(userId); // Kilitler gitti
        refreshTokenRepository.deleteAllByUserId(userId);
        securityCodeRepository.deleteAllByUserId(userId);
        goalRepository.deleteAllByUserId(userId); // Hedefler gitti
        passwordResetTokenRepository.deleteAllByUserId(userId); // Şifre sıfırlama tokenları gitti
        userLayoutRepository.deleteByUserId(userId); // Kullanıcı arayüz yerleşimi gitti
        userDeviceTokenRepository.deleteAllByUserId(userId); // Cihaz bildirim tokenları gitti

        // 2. Tüm veriler temizlendikten sonra en son kullanıcıyı sil
        userRepository.delete(user);
    }
}