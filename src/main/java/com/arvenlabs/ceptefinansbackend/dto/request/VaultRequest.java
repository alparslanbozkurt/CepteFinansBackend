package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VaultRequest {
    private Long categoryId;
    private BigDecimal amount;
}
