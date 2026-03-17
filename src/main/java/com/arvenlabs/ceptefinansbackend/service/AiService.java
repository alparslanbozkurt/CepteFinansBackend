package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.response.ai.AiRoastResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Transaction;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import com.arvenlabs.ceptefinansbackend.repository.TransactionRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    public AiRoastResponse generateRoast() {
        User user = getCurrentUser();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        List<Transaction> recentTransactions = transactionRepository
                .findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(user.getId(), startDate, endDate)
                .stream()
                .filter(t -> t.getType() == CategoryType.EXPENSE)
                .collect(Collectors.toList());

        if (recentTransactions.isEmpty()) {
            return AiRoastResponse.builder()
                    .roast("Son 7 günde hiç harcama yapmamışsın. Ya evden çıkmıyorsun ya da başkalarına ödetiyorsun. Klasik Pinti!")
                    .score(10)
                    .build();
        }

        String transactionSummary = recentTransactions.stream()
                .map(t -> t.getTransactionDate() + " tarihinde " + t.getCategory().getName() + " için " + t.getAmount() + " TL harcadı. (" + t.getDescription() + ")")
                .collect(Collectors.joining("\n"));

        String systemPrompt = buildPrompt(transactionSummary, user.getFullName(), user.getIncome() != null ? user.getIncome().toString() : "Bilinmiyor");

        try {
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

            return AiRoastResponse.builder()
                    .roast(data.path("roast").asText())
                    .score(data.path("score").asInt(5))
                    .build();

        } catch (Exception e) {
            log.error("AI Roast hatası:", e);
            throw new RuntimeException("AI seni gömerken bir hata oluştu: " + e.getMessage());
        }
    }

    private String buildPrompt(String transactions, String name, String income) {
        return "Sen laubali, sarkastik, acımasız ve laf sokan bir finansal yapay zeka asistanısın (Financial AI Roaster). " +
                "Kullanıcı " + name + "'in son 7 günlük harcamalarını ve " + income + " TL aylık gelirini (bilinmiyorsa yoksay) aşağıda veriyorum. " +
                "Buna göre kullanıcıya çok sağlam, acımasız ve komik bir şekilde laf sok (maksimum 4-5 cümle). " +
                "Kullanıcının harcama alışkanlıklarıyla dalga geç. " +
                "Ayrıca kullanıcıya 1 ile 10 arasında bir 'Fakirlik/Savurganlık' skoru ver (1: Ultra Fakir/Pinti, 10: Mirasyedi Savurgan). " +
                "Dönüş SADECE saf JSON formatında olmalı. " +
                "{\n  \"roast\": \"...\",\n  \"score\": 8\n}\n\n" +
                "İşte Kullanıcının Harcamaları:\n" + transactions;
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
