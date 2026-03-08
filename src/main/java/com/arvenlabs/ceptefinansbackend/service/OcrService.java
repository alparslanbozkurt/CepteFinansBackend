package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.response.OcrResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OcrResponse scanReceipt(MultipartFile file) {
        try {
            log.info("📸 Gemini AI ile Fiş okuma işlemi başlatıldı...");

            // 1. Resmi Base64 formatına çevir
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();

            // 2. Yapay Zekaya verilecek kesin Prompt
            String systemPrompt = "Sen uzman bir finans asistanısın ve fiş okuma (OCR) görevin var. " +
                    "Sana verilen fiş görüntüsünden aşağıdaki bilgileri çıkar ve SADECE JSON dön. " +
                    "Kurallar: " +
                    "1. amount: Fişteki TOPLAM tutar (sadece rakam, örneğin 150.50). " +
                    "2. transactionDate: Fişin tarihi (KESİNLİKLE YYYY-MM-DD formatında, örneğin 2024-03-08). Eğer tarihi bulamazsan bugünün tarihini at. " +
                    "3. description: Fişin kesildiği kurumun/marketin adı (Örn: Migros, Starbucks). " +
                    "4. suggestedCategory: Harcamanın türü (Şunlardan biri olmalı: MARKET, RESTORAN, YAKIT, SAGLIK, GIYIM, FATURA, DIGER).";

            // 3. Google Gemini API JSON Payload'ını Hazırla
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", systemPrompt);

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType);
            inlineData.put("data", base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("inlineData", inlineData);

            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("parts", List.of(textPart, imagePart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contentPart));

            // Gemini 2.5 özelliğidir: AI'ı kesinlikle JSON dönmeye zorlar!
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            requestBody.put("generationConfig", generationConfig);

            // 4. API İsteğini Gönder (Gemini, API anahtarını URL'de bekler)
            String urlWithKey = geminiApiUrl + "?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, requestEntity, String.class);

            // 5. Gelen Cevabı Parse Et
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            // Gemini'nin JSON yapısında cevabın bulunduğu yer: candidates[0].content.parts[0].text
            String aiResponseContent = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();

            log.info("✅ Gemini Fişi Başarıyla Okudu: {}", aiResponseContent);

            JsonNode extractedData = objectMapper.readTree(aiResponseContent);

            // 6. OcrResponse objesine çevir ve döndür
            return OcrResponse.builder()
                    .amount(new BigDecimal(extractedData.path("amount").asText("0")))
                    .transactionDate(LocalDate.parse(extractedData.path("transactionDate").asText(), DateTimeFormatter.ISO_LOCAL_DATE))
                    .description(extractedData.path("description").asText())
                    .suggestedCategory(extractedData.path("suggestedCategory").asText())
                    .build();

        } catch (Exception e) {
            log.error("❌ Gemini API Hatası Detayı: ", e);
            throw new RuntimeException("YZ API Hatası Detayı: " + e.getMessage());
        }
    }
}