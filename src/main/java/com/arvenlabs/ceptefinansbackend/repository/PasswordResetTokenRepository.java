package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.PasswordResetToken;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    // Gelen token string'ine göre veritabanında arama yapmak için
    Optional<PasswordResetToken> findByToken(String token);

    // Kullanıcıya ait mevcut token'ı bulmak için
    Optional<PasswordResetToken> findByUser(User user);

    void deleteAllByUserId(UUID userId);
}