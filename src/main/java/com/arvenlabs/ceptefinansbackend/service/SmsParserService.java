package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.SmsAnalysisRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.SmsAnalysisResponse;
import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsParserService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SmsAnalysisResponse parseSms(SmsAnalysisRequest request) {
        try {
            log.info("📩 SMS Analizi Başlatıldı. Gönderen: {}", request.getSender());

            String today = LocalDate.now().toString();

            String systemPrompt = buildPrompt(request, today);

            Map<String, Object> requestBody = buildRequestBody(systemPrompt);

            String urlWithKey = geminiApiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, httpRequest, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String aiResponseText = rootNode
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            log.info("✅ Gemini SMS'i Başarıyla Okudu: {}", aiResponseText);

            JsonNode data = objectMapper.readTree(aiResponseText);

            return SmsAnalysisResponse.builder()
                    .amount(new BigDecimal(data.path("amount").asText("0")))
                    .transactionDate(LocalDate.parse(data.path("transactionDate").asText(), DateTimeFormatter.ISO_LOCAL_DATE))
                    .description(data.path("description").asText())
                    .suggestedCategory(data.path("suggestedCategory").asText())
                    .type(CategoryType.valueOf(data.path("type").asText("EXPENSE")))
                    .build();

        } catch (Exception e) {
            log.error("❌ SMS Okunurken Hata: ", e);
            throw new RuntimeException("SMS Analiz Hatası: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private String buildPrompt(SmsAnalysisRequest request, String today) {
        return "Sen uzman bir finans asistanısın. Sana verilen SMS bilgilerinden harcama/gelir detaylarını çıkar " +
                "ve SADECE JSON formatında dön:\n\n" +
                "1. amount          : İşlem tutarı (sadece rakam, örneğin 150.50).\n" +
                "2. transactionDate : İşlem tarihi (KESİNLİKLE YYYY-MM-DD formatında). " +
                "Eğer tarih yoksa bugünün tarihini (" + today + ") kullan.\n" +
                "3. description     : İşlemin yapıldığı yer, gönderen kişi veya kurum (Örn: Migros, Ali Yılmaz). " +
                "Dikkat: 'Ziraat', 'Garanti' gibi banka isimlerini değil, " +
                "metin içindeki asıl harcama yapılan veya parayı gönderen yeri bul.\n" +
                "4. suggestedCategory: Harcamanın/gelirin türü " +
                "(MARKET, RESTORAN, YAKIT, SAGLIK, GIYIM, FATURA, MAAS, TRANSFER, DIGER).\n" +
                "5. type            : Hesaba gelen paraysa 'INCOME', çıkan/harcanan paraysa 'EXPENSE'.\n\n" +
                "--- SMS BİLGİLERİ ---\n" +
                "Gönderen (Sender): " + request.getSender() + "\n" +
                "Mesaj Metni      : " + request.getSmsText();
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("parts", List.of(textPart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(contentPart));
        body.put("generationConfig", generationConfig);

        return body;
    }
}
