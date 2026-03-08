package com.arvenlabs.ceptefinansbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OcrResponse {
    private BigDecimal amount;          // Okunan Toplam Tutar
    private LocalDate transactionDate;  // Okunan Tarih
    private String description;         // Fişin kesildiği yer (Örn: Migros, Shell vb.)
    private String suggestedCategory;   // YZ'nin önerdiği kategori (Örn: MARKET, YAKIT)
}