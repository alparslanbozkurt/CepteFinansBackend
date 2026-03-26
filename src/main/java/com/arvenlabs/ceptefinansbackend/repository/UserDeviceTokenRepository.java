package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

    /** Kullanıcıya ait tüm cihaz token'larını getirir (FCM broadcast için) */
    List<UserDeviceToken> findAllByUser(User user);

    /** Token string'i ile kayıt bul (upsert kontrolü) */
    Optional<UserDeviceToken> findByFcmToken(String fcmToken);

    /** Kullanıcı + platform kombinasyonuna göre kayıt bul (upsert için) */
    Optional<UserDeviceToken> findByUserAndPlatform(User user, String platform);
}
