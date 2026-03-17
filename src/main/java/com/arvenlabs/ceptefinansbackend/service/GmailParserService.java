package com.arvenlabs.ceptefinansbackend.service;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailParserService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SmsAnalysisResponse> parseEmails(List<String> emailContents) {
        List<SmsAnalysisResponse> parsedResults = new ArrayList<>();
        if (emailContents == null || emailContents.isEmpty()) {
            return parsedResults;
        }

        log.info("📧 Toplam {} adet şüpheli e-posta Gemini ile analiz ediliyor...", emailContents.size());

        for (String content : emailContents) {
            try {
                SmsAnalysisResponse response = analyzeSingleEmail(content);
                if (response != null && response.getAmount() != null && response.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    parsedResults.add(response);
                }
            } catch (Exception e) {
                log.warn("Bir e-posta parse edilemedi, geçiliyor... Detay: {}", e.getMessage());
            }
        }
        
        log.info("✅ {} e-postadan {} adet tutarlı harcama/fatura çıkarıldı.", emailContents.size(), parsedResults.size());
        return parsedResults;
    }

    private SmsAnalysisResponse analyzeSingleEmail(String emailContent) {
        try {
            String today = LocalDate.now().toString();
            String systemPrompt = buildPrompt(emailContent, today);
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

            if (aiResponseText.startsWith("```json")) {
                aiResponseText = aiResponseText.replace("```json", "");
            }
            if (aiResponseText.endsWith("```")) {
                aiResponseText = aiResponseText.substring(0, aiResponseText.lastIndexOf("```"));
            }

            JsonNode data = objectMapper.readTree(aiResponseText.trim());

            BigDecimal amount = new BigDecimal(data.path("amount").asText("0"));
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }

            String dateStr = data.path("transactionDate").asText();
            LocalDate transactionDate;
            try {
                transactionDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                transactionDate = LocalDate.now();
            }

            return SmsAnalysisResponse.builder()
                    .amount(amount)
                    .transactionDate(transactionDate)
                    .description(data.path("description").asText())
                    .suggestedCategory(data.path("suggestedCategory").asText("DIGER"))
                    .type(CategoryType.valueOf(data.path("type").asText("EXPENSE")))
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(String emailContent, String today) {
        if (emailContent != null && emailContent.length() > 3000) {
            emailContent = emailContent.substring(0, 3000);
        }

        return "Sen uzman bir finans asistanısın. Sana verilen E-POSTA FATURA içeriğinden harcama/gelir detaylarını çıkar " +
                "ve SADECE JSON formatında dön. Başka hiçbir açıklama ekleme:\n\n" +
                "1. amount          : İşlem tutarı (sadece rakam, örneğin 150.50). KDV falan değil, ödenen TOPLAM TUTAR.\n" +
                "2. transactionDate : İşlem/fatura tarihi (KESİNLİKLE YYYY-MM-DD formatında). " +
                "Eğer tarih yoksa bugünün tarihini (" + today + ") kullan.\n" +
                "3. description     : İşlemin yapıldığı yer veya fatura kurumu (Örn: Trendyol, Amazon, İGDAŞ, Turkcell). " +
                "Kişisel bilgileri veya alakasız metinleri dahil etme.\n" +
                "4. suggestedCategory: Harcamanın/gelirin türü " +
                "(MARKET, RESTORAN, YAKIT, SAGLIK, GIYIM, FATURA, MAAS, TRANSFER, DIGER).\n" +
                "5. type            : Fatura veya harcamaysa 'EXPENSE', hesaba giren bir paraysa 'INCOME'.\n\n" +
                "Eğer e-posta içinde herhangi bir finansal işlem/fatura (ürün, hizmet ödemesi) MİKTARI yoksa amount için 0 yaz.\n\n" +
                "--- E-POSTA METNİ ---\n" +
                emailContent;
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
