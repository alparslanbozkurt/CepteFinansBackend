package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoalRequest {
    private String title;
    private BigDecimal targetAmount;
    private LocalDate deadline;
}
