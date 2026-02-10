package com.arvenlabs.ceptefinansbackend.dto.response;

import com.arvenlabs.ceptefinansbackend.model.enums.BudgetPeriod;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {
    private UUID id;
    private String categoryName;
    private BigDecimal limitAmount; // Hedeflenen limit
    private BigDecimal spentAmount; // Şu ana kadar harcanan (Bunu hesaplayacağız!)
    private Double percentage;      // Yüzde kaçı doldu? (%80 gibi)
    private BudgetPeriod period;
    private LocalDate startDate;
    private LocalDate endDate;
}