package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;

@Data
public class GmailAnalysisRequest {
    private String accessToken; // Google OAuth2 Access Token
    private Integer daysToScan; // Optional: Kaç günlük e-postaların taranacağı (varsayılan: 7)
}
