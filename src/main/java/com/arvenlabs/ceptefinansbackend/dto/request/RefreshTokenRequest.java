package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String token;
}