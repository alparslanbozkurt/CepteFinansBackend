package com.arvenlabs.ceptefinansbackend.dto.response;

import com.arvenlabs.ceptefinansbackend.model.enums.LockStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VaultResponse {
    private UUID id;
    private String categoryName;
    private BigDecimal amount;
    private LockStatus status;
    private LocalDateTime lockedAt;
    private LocalDateTime unlocksAt;
}
