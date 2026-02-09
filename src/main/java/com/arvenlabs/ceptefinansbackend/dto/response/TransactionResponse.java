package com.arvenlabs.ceptefinansbackend.dto.response;

import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private String categoryName;  // ID değil, isim dönüyoruz (Örn: "Market")
    private String categoryIcon;  // İkonu da dönelim ki ekranda güzel gözüksün
    private CategoryType type;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
}