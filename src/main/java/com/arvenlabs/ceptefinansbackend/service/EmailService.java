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

    // YENİ: application.properties'den frontend (Next.js) adresini çekiyoruz
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
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

    // YENİ EKLENEN METOT: Şifre Sıfırlama Linki Gönderici
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            log.info("📧 Şifre sıfırlama e-postası başlatıldı -> Hedef: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Cepte Finans - Şifre Sıfırlama Talebi");

            // Yönlendirme Linkini Oluştur (Örn: http://localhost:3000/reset-password?token=b7a1...)
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String mailContent = "Merhaba,\n\n"
                    + "Cepte Finans hesabınız için şifre sıfırlama talebinde bulundunuz.\n"
                    + "Aşağıdaki bağlantıya tıklayarak yeni şifrenizi belirleyebilirsiniz:\n\n"
                    + resetLink + "\n\n"
                    + "Bu bağlantı 15 dakika boyunca geçerlidir.\n"
                    + "Eğer bu talebi siz yapmadıysanız, bu e-postayı görmezden gelebilir ve hesabınızı güvenle kullanmaya devam edebilirsiniz.\n\n"
                    + "Güvenli günler dileriz,\n"
                    + "Arven Labs Ekibi";

            message.setText(mailContent);
            mailSender.send(message);

            log.info("✅ Şifre sıfırlama e-postası başarıyla gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Şifre sıfırlama e-postası gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    @Async
    public void sendEmail(String toEmail, String subject, String text) {
        try {
            log.info("📧 Genel e-posta gönderme işlemi başlatıldı -> Hedef: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("✅ E-posta başarıyla gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ E-posta gönderilirken kritik hata oluştu: {}", e.getMessage());
        }
    }
}