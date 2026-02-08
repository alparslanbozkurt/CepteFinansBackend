package com.arvenlabs.ceptefinansbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;       // Access Token
    private String refreshToken;
    private String message;     // "Giriş Başarılı" vb.
}