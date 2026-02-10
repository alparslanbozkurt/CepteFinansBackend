package com.arvenlabs.ceptefinansbackend.dto.request;

import com.arvenlabs.ceptefinansbackend.model.enums.BudgetPeriod;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetRequest {
    private Long categoryId;
    private BigDecimal amount;
    private BudgetPeriod period; // Gönderilmezse MONTHLY varsayacağız
}