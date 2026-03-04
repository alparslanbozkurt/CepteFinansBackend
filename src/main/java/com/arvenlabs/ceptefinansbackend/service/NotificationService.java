package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.response.NotificationResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Notification;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.NotificationRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 1. Backend'in içinden çağrılacak metod (TransactionService vb. kullanacak)
    public void createAndSendNotification(User user, String title, String message) {
        // A) Veritabanına kaydet
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        log.info("🔔 Bildirim Kaydedildi -> Kime: {} | Mesaj: {}", user.getEmail(), message);

        // B) TODO: İleride buraya Android/FCM (Firebase) Push Notification kodları gelecek.
        // C) TODO: İleride Web tarafı için WebSocket (Canlı güncelleme) tetiklenecek.
    }

    // 2. Kullanıcının bildirimlerini listeleme (Web/Mobil çağıracak)
    public List<NotificationResponse> getUserNotifications() {
        User user = getCurrentUser();
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. Bildirimi "Okundu" olarak işaretleme
    public void markAsRead(UUID notificationId) {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Bildirim bulunamadı"));

        // Güvenlik: Başkasının bildirimini okundu yapamasın
        if (notification.getUser().getId().equals(user.getId())) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    // 4. Okunmamış bildirim sayısını getirme (Zil 🔔 rozeti için)
    public long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}