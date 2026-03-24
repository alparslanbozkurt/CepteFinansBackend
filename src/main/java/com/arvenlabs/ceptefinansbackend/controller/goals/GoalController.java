package com.arvenlabs.ceptefinansbackend.controller.goals;

import com.arvenlabs.ceptefinansbackend.dto.request.GoalRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.dto.response.GoalResponse;
import com.arvenlabs.ceptefinansbackend.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(@RequestBody GoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(goalService.createGoal(request), "Finansal hedef başarıyla oluşturuldu."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals() {
        return ResponseEntity.ok(ApiResponse.success(goalService.getMyGoals(), "Hedefler başarıyla getirildi."));
    }

    @PostMapping("/{id}/add-savings")
    public ResponseEntity<ApiResponse<GoalResponse>> addSavings(@PathVariable UUID id, @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal amount = payload.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Geçerli bir birikim miktarı giriniz.");
        }
        return ResponseEntity.ok(ApiResponse.success(goalService.addSavings(id, amount), "Birikim hedefinize başarıyla eklendi."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(@PathVariable UUID id, @RequestBody GoalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(goalService.updateGoal(id, request), "Hedef başarıyla güncellendi."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable UUID id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Hedef başarıyla silindi."));
    }
}
