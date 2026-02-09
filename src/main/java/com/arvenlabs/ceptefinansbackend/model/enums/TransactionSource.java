package com.arvenlabs.ceptefinansbackend.model.enums;

public enum TransactionSource {
    MANUAL, // Elle girilen
    SMS,    // Banka SMS'inden gelen
    OCR     // Fiş tarama ile gelen
}