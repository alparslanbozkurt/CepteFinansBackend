package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;

@Data
public class VerifyEmailRequest {
    private String email;
    private String code;
}