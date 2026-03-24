package com.arvenlabs.ceptefinansbackend.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FCMService {

    public void sendPushNotification(String targetDeviceToken, String title, String body) {
        // Kullanıcının ekranına düşecek bildirimin başlığı ve içeriği (notification payload)
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // Android için yüksek öncelikli bildirim ayarı
        // Bu sayede uygulama arka planda veya kapalıyken de bildirim sistem tepsisine düşer
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setSound("default")
                        .setChannelId("default_channel") // Android 8+ için bildirim kanalı
                        .build())
                .build();

        // iOS (APNs) için bildirim ayarı
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .build())
                .build();

        // Bildirimi, data payload'ı ve platform ayarlarını paketliyoruz
        Message message = Message.builder()
                .setToken(targetDeviceToken)
                .setNotification(notification)        // notification payload (arka plan bildirimi için şart)
                .setAndroidConfig(androidConfig)       // Android'e özel yüksek öncelik
                .setApnsConfig(apnsConfig)             // iOS desteği
                .putData("title", title)               // data payload (uygulama açıkken kullanılır)
                .putData("body", body)
                .putData("click_action", "OPEN_MAIN_ACTIVITY")
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