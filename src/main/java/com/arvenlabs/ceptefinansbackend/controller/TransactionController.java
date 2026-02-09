package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.dto.request.TransactionRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.TransactionResponse;
import com.arvenlabs.ceptefinansbackend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // 1. Ekleme (POST) - ZATEN VARDI
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(@RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.createTransaction(request), "Kayıt Başarılı"));
    }

    // 2. Listeleme ve Filtreleme (GET) - GÜNCELLENDİ
    // Kullanım: /api/v1/transactions?startDate=2024-01-01&endDate=2024-01-31
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Eğer tarih verilmişse filtrele, verilmemişse hepsini getir
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    transactionService.getTransactionsByDateRange(startDate, endDate), "Filtrelenmiş işlemler"
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(transactionService.getAllTransactions(), "Tüm işlemler"));
    }

    // 3. Güncelleme (PUT) - YENİ
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable UUID id,
            @RequestBody TransactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.updateTransaction(id, request), "İşlem güncellendi"));
    }

    // 4. Silme (DELETE) - ZATEN VARDI
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(null, "İşlem silindi"));
    }
}