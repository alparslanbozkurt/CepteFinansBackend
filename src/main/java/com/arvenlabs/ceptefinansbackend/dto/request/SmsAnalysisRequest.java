package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;

@Data
public class SmsAnalysisRequest {
    private String sender;  // SMS'i gönderen (Örn: ZIRAAT, ENPARA, 532xxxxxxx)
    private String smsText; // SMS'in asıl metni
}