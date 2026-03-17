package com.arvenlabs.ceptefinansbackend.controller.habits;

import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import com.arvenlabs.ceptefinansbackend.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @GetMapping("/streak")
    public ResponseEntity<ApiResponse<List<Boolean>>> getStreakMap() {
        return ResponseEntity.ok(ApiResponse.success(habitService.getStreakMap(), "30 günlük alışkanlık serisi başarıyla getirildi."));
    }
}
