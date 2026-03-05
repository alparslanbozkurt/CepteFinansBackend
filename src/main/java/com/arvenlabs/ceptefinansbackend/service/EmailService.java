package com.arvenlabs.ceptefinansbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // JavaMailSender yerine REST API kullanıyoruz
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Zoho API ayarları (application.properties'e eklenecek)
    @Value("${zoho.account.id:123456789}")
    private String accountId;

    @Value("${zoho.api.access-token:DUMMY_TOKEN}")
    private String zohoAccessToken;

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        log.info("📧 API ile Doğrulama E-postası başlatıldı -> Hedef: {}", toEmail);

        String subject = "Cepte Finans - Güvenlik Kodunuz";
        String content = "Merhaba,\n\n"
                + "Cepte Finans'a hoş geldiniz! Hesabınızı güvenle kullanmaya başlamak için doğrulama kodunuz:\n\n"
                + "GÜVENLİK KODU: " + code + "\n\n"
                + "Bu kod 5 dakika boyunca geçerlidir. Lütfen bu kodu kimseyle paylaşmayın.\n\n"
                + "İyi günler dileriz,\n"
                + "Arven Labs Ekibi";

        sendEmailViaZohoApi(toEmail, subject, content);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("📧 API ile Şifre sıfırlama e-postası başlatıldı -> Hedef: {}", toEmail);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Cepte Finans - Şifre Sıfırlama Talebi";
        String content = "Merhaba,\n\n"
                + "Cepte Finans hesabınız için şifre sıfırlama talebinde bulundunuz.\n"
                + "Aşağıdaki bağlantıya tıklayarak yeni şifrenizi belirleyebilirsiniz:\n\n"
                + resetLink + "\n\n"
                + "Bu bağlantı 15 dakika boyunca geçerlidir.\n"
                + "Eğer bu talebi siz yapmadıysanız, bu e-postayı görmezden gelebilir ve hesabınızı güvenle kullanmaya devam edebilirsiniz.\n\n"
                + "Güvenli günler dileriz,\n"
                + "Arven Labs Ekibi";

        sendEmailViaZohoApi(toEmail, subject, content);
    }

    @Async
    public void sendEmail(String toEmail, String subject, String text) {
        sendEmailViaZohoApi(toEmail, subject, text);
    }

    private void sendEmailViaZohoApi(String toEmail, String subject, String content) {
        try {
            // Zoho API URL
            String url = "https://mail.zoho.eu/api/accounts/" + accountId + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Zoho-oauthtoken " + zohoAccessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("fromAddress", fromEmail);
            body.put("toAddress", toEmail);
            body.put("subject", subject);
            body.put("content", content);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.info("✅ Zoho API ile e-posta başarıyla uçuruldu: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Zoho API ile e-posta gönderilirken hata oluştu: {}", e.getMessage());
        }
    }
}