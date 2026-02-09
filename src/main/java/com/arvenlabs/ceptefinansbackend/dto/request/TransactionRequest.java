package com.arvenlabs.ceptefinansbackend.dto.request;

import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import com.arvenlabs.ceptefinansbackend.model.enums.TransactionSource;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {
    private Long categoryId;      // Hangi kategori? (ID olarak gelecek)
    private CategoryType type;    // Gelir mi Gider mi?
    private BigDecimal amount;    // Tutar
    private String description;   // Açıklama
    private LocalDate transactionDate; // İşlem Tarihi

    // Varsayılan olarak MANUAL kabul edeceğiz, ama gönderilirse işleriz.
    private TransactionSource source;
}