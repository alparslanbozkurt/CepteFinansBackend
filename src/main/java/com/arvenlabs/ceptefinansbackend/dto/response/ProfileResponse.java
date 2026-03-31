package com.arvenlabs.ceptefinansbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String fullName;
    private String email;
    private Integer age;
    private BigDecimal income;
    private String occupation;
    private String profileImageUrl;
    private LocalDateTime createdAt;
}
