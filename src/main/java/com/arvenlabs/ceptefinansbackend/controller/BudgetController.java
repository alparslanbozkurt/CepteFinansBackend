package com.arvenlabs.ceptefinansbackend.controller;

import com.arvenlabs.ceptefinansbackend.dto.request.BudgetRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.BudgetResponse;
import com.arvenlabs.ceptefinansbackend.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> create(@RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.createBudget(request), "Bütçe oluşturuldu"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgets(), "Bütçeler listelendi"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bütçe silindi"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgetById(id), "Bütçe detayları"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> update(@PathVariable UUID id,
            @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.updateBudget(id, request), "Bütçe güncellendi"));
    }
}