package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Kullanıcının bildirimlerini tarihe göre azalan (en yeni en üstte) getir
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteAllByUserId(UUID userId);
    // Okunmamış bildirim sayısını getir (Zil ikonundaki sayı için: Örn: 🔔 3)
    long countByUserIdAndIsReadFalse(UUID userId);
}