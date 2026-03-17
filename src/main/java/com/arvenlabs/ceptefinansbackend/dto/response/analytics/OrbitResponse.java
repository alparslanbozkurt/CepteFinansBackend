package com.arvenlabs.ceptefinansbackend.dto.response.analytics;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrbitResponse {
    private String planetName;
    private BigDecimal currentAmount;
    private BigDecimal limit;
    private long orbitSpeedMs;
    private boolean isVibrating;
}
