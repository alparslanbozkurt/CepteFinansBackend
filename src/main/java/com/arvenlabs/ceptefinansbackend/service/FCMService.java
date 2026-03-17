package com.arvenlabs.ceptefinansbackend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FCMService {

    public void sendPushNotification(String targetDeviceToken, String title, String body) {
        // Kullanıcının ekranına düşecek bildirimin başlığı ve içeriği
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // Bildirimi ve kime gideceğini (Token) paketliyoruz
        Message message = Message.builder()
                .setToken(targetDeviceToken)
                .setNotification(notification)
                .build();

        try {
            // Firebase'e fırlatıyoruz
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Başarıyla gönderilen mesaj ID'si: " + response);
        } catch (Exception e) {
            System.err.println("❌ FCM mesajı gönderilirken hata: " + e.getMessage());
        }
    }
}