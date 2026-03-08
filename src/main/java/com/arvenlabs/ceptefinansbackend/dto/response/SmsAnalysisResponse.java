package com.arvenlabs.ceptefinansbackend.dto.response;

import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SmsAnalysisResponse {
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String description;
    private String suggestedCategory;
    private CategoryType type; // INCOME veya EXPENSE
}