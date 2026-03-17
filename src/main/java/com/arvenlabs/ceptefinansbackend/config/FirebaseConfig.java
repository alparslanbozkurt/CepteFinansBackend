package com.arvenlabs.ceptefinansbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;

                // 1. Önce Ortam Değişkenine (Environment Variable) bak (Railway için)
                String firebaseEnvJson = System.getenv("FIREBASE_CREDENTIALS_JSON");

                if (firebaseEnvJson != null && !firebaseEnvJson.trim().isEmpty()) {
                    // Railway'de isek, değişkendeki metni akışa (InputStream) çevir
                    serviceAccount = new ByteArrayInputStream(firebaseEnvJson.getBytes(StandardCharsets.UTF_8));
                    System.out.println("☁️ Firebase ayarları Ortam Değişkeninden (Railway) yüklendi.");
                } else {
                    // 2. Eğer ortam değişkeni yoksa, dosyadan oku (Kendi bilgisayarın için)
                    serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
                    System.out.println("💻 Firebase ayarları yerel JSON dosyasından yüklendi.");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase Admin SDK başarıyla başlatıldı!");
            }
        } catch (Exception e) {
            System.err.println("❌ Firebase başlatılamadı: " + e.getMessage());
        }
    }
}
