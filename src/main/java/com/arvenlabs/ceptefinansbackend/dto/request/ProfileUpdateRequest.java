package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfileUpdateRequest {
    private String fullName;
    private Integer age;
    private BigDecimal income;
    private String occupation;
}
