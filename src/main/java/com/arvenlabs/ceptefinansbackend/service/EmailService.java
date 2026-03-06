package com.arvenlabs.ceptefinansbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Zoho API ayarları
    @Value("${zoho.account.id:}")
    private String accountId;

    @Value("${zoho.client.id:}")
    private String clientId;

    @Value("${zoho.client.secret:}")
    private String clientSecret;

    @Value("${zoho.refresh.token:}")
    private String refreshToken;

    private String validAccessToken = null;
    private long tokenExpiryTime = 0;

    /**
     * Thread-safe access token retrieval.
     * Mevcut token'ın süresinin dolup dolmadığını kontrol eder, dolmuşsa yeni bir
     * access token alır.
     */
    private synchronized String getValidAccessToken() {
        // Süresinin dolmasına 60 saniye kalmışsa bile yeniliyoruz ki garanti olsun
        if (validAccessToken == null || Instant.now().getEpochSecond() >= (tokenExpiryTime - 60)) {
            refreshAccessToken();
        }
        return validAccessToken;
    }

    private void refreshAccessToken() {
        log.info("🔄 Zoho OAuth2 Access Token süresi doldu veya mevcut değil. Yeni token alınıyor...");
        try {
            String url = "https://accounts.zoho.eu/oauth/v2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("refresh_token", refreshToken);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> bodyMap = response.getBody();

                Object accessTokenObj = bodyMap.get("access_token");
                if (accessTokenObj != null) {
                    validAccessToken = String.valueOf(accessTokenObj);
                } else {
                    log.error("❌ Zoho API 'access_token' dönmedi! Dönen tam yanıt: {}", bodyMap);
                    return;
                }

                int expiresIn = 3600; // Default 1 hour fallback
                Object expiresInObj = bodyMap.get("expires_in");
                if (expiresInObj instanceof Integer) {
                    expiresIn = (Integer) expiresInObj;
                } else if (expiresInObj instanceof String) {
                    try {
                        expiresIn = Integer.parseInt((String) expiresInObj);
                    } catch (NumberFormatException ignored) {
                    }
                }

                tokenExpiryTime = Instant.now().getEpochSecond() + expiresIn;
                log.info("✅ Yeni Zoho Access Token başarıyla alındı. Geçerlilik süresi: {} saniye", expiresIn);
            } else {
                log.error("❌ Zoho Access Token alınırken hata oluştu. Status Code: {}, Response: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Zoho Access Token refresh işleminde istisna: {}", e.getMessage());
        }
    }

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
        String token = getValidAccessToken();
        if (token == null) {
            log.error("❌ Geçerli bir Access Token bulunamadığı için e-posta gönderilemedi!");
            return;
        }

        try {
            // Zoho API URL
            String url = "https://mail.zoho.eu/api/accounts/" + accountId + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Zoho-oauthtoken " + token);

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
                    String.class);

            log.info("✅ Zoho API ile e-posta başarıyla uçuruldu: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Zoho API ile e-posta gönderilirken hata oluştu: {}", e.getMessage());
        }
    }
}