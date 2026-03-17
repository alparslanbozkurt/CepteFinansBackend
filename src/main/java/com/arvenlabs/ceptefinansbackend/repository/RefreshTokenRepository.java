package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.RefreshToken;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    // Bir kullanıcının tüm tokenlarını bul (Logout yaparken hepsini silmek gerekebilir)
    void deleteAllByUserId(UUID userId);
    @Transactional
    // Silme işlemi için bu anotasyon şart!
    void deleteByUser(User user);

    @Transactional
    void deleteByToken(String token);
}