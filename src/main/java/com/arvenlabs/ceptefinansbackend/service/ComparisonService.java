package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.model.entity.Category;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.CategoryRepository;
import com.arvenlabs.ceptefinansbackend.repository.TransactionRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // Sadece properties dosyasında hali hazırda var olan değişkenleri alıyoruz
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public String getComparisonInsights() {
        User user = getCurrentUser();
        if (user.getAge() == null || user.getIncome() == null || user.getOccupation() == null) {
            return "Lütfen profilinizden yaş, gelir ve meslek bilgilerinizi güncelleyin.";
        }

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

        List<Category> allCategories = categoryRepository.findAll();
        StringBuilder comparisonData = new StringBuilder();

        for (Category category : allCategories) {
            if (category.getType().name().equals("EXPENSE")) {
                Double userTotal = transactionRepository.findTotalExpenseByCategoryAndDate(
                        user.getId(), category.getId(), startDate, endDate);

                // Demographic ranges
                int minAge = user.getAge() - 5;
                int maxAge = user.getAge() + 5;
                BigDecimal minIncome = user.getIncome().multiply(new BigDecimal("0.8"));
                BigDecimal maxIncome = user.getIncome().multiply(new BigDecimal("1.2"));

                Double avgTotal = transactionRepository.findAverageSpendingByDemographics(
                        user.getId(), category.getId(), minAge, maxAge, minIncome, maxIncome, user.getOccupation(),
                        startDate, endDate);

                if (userTotal != null && avgTotal != null && avgTotal > 0) {
                    double percentageDiff = ((userTotal - avgTotal) / avgTotal) * 100;
                    comparisonData.append(String.format("- %s kategorisinde harcaman ortalamadan %%.2f %s.\n",
                            category.getName(), Math.abs(percentageDiff), percentageDiff > 0 ? "fazla" : "az"));
                }
            }
        }

        if (comparisonData.length() == 0) {
            return "Harcama verileriniz henüz anonim havuzla karşılaştırılacak kadar yeterli değil.";
        }

        return generateAIInsights(comparisonData.toString(), user);
    }

    private String generateAIInsights(String data, User user) {
        // Properties'den gelen URL'nin sonuna sadece API Key'i parametre olarak ekliyoruz
        String fullUrl = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = String.format(
                "Kullanıcı Bilgileri: Yaş %d, Meslek %s, Gelir %.2f TL.\n" +
                        "Kullanıcının bu ayki harcama karşılaştırmaları:\n%s\n" +
                        "Bu verilere dayanarak kullanıcıya ufuk açıcı, samimi ve finansal farkındalık yaratan 2-3 cümlelik bir içgörü sun. "
                        +
                        "Kullanıcının dilinde konuş (Türkçe).",
                user.getAge(), user.getOccupation(), user.getIncome().doubleValue(), data);

        // Gemini JSON Formatına uygun Body oluşturma
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(fullUrl, entity, Map.class);

            // Gemini JSON yanıtını çözümleme
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                    List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");
                    return (String) responseParts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini API İstek Hatası: " + e.getMessage());
            return "İçgörü oluşturulurken bir hata oluştu: " + data;
        }

        return data;
    }

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}