package com.arvenlabs.ceptefinansbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // Sadece bir kere başlatıldığından emin oluyoruz
            if (FirebaseApp.getApps().isEmpty()) {
                // resources klasöründeki dosyayı okuyoruz
                InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();

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