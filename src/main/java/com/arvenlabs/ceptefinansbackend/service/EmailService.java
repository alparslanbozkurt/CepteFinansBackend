package com.arvenlabs.ceptefinansbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // Spring Boot'un bize sağladığı mail atma motoru
    private final JavaMailSender mailSender;

    // application.properties'den gönderici adresini çekiyoruz (Örn: iletisim@arvenlabs.com)
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async // Bu metod çağrıldığında ana akışı beklemez, arka planda kendi kendine çalışır!
    public void sendVerificationEmail(String toEmail, String code) {
        try {
            log.info("📧 E-posta gönderme işlemi başlatıldı -> Hedef: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Cepte Finans - Güvenlik Kodunuz");

            // Mailin içeriği
            String mailContent = "Merhaba,\n\n"
                    + "Cepte Finans'a hoş geldiniz! Hesabınızı güvenle kullanmaya başlamak için doğrulama kodunuz:\n\n"
                    + "GÜVENLİK KODU: " + code + "\n\n"
                    + "Bu kod 5 dakika boyunca geçerlidir. Lütfen bu kodu kimseyle paylaşmayın.\n\n"
                    + "İyi günler dileriz,\n"
                    + "Arven Labs Ekibi";

            message.setText(mailContent);

            mailSender.send(message);
            log.info("✅ Doğrulama e-postası başarıyla gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ E-posta gönderilirken kritik hata oluştu: {}", e.getMessage());
        }
    }
}