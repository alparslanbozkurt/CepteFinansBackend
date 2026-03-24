package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.GoalRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.GoalResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Goal;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.GoalRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User user = getCurrentUser();

        Goal goal = Goal.builder()
                .user(user)
                .title(request.getTitle())
                .targetAmount(request.getTargetAmount())
                .savedAmount(BigDecimal.ZERO)
                .deadline(request.getDeadline())
                .build();

        Goal saved = goalRepository.save(goal);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getMyGoals() {
        User user = getCurrentUser();
        return goalRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GoalResponse addSavings(UUID goalId, BigDecimal amount) {
        User user = getCurrentUser();
        Goal goal = goalRepository.findById(goalId).orElseThrow(() -> new RuntimeException("Hedef bulunamadı"));
        
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu hedef üzerinde işlem yapma yetkiniz yok.");
        }

        goal.setSavedAmount(goal.getSavedAmount().add(amount));
        Goal saved = goalRepository.save(goal);
        return mapToResponse(saved);
    }

    @Transactional
    public GoalResponse updateGoal(UUID goalId, GoalRequest request) {
        User user = getCurrentUser();
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Hedef bulunamadı"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu hedef üzerinde işlem yapma yetkiniz yok.");
        }

        goal.setTitle(request.getTitle());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDeadline(request.getDeadline());

        Goal saved = goalRepository.save(goal);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteGoal(UUID goalId) {
        User user = getCurrentUser();
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Hedef bulunamadı"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu hedef üzerinde işlem yapma yetkiniz yok.");
        }

        goalRepository.delete(goal);
    }

    private GoalResponse mapToResponse(Goal goal) {
        int percentage = 0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = goal.getSavedAmount()
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .intValue();
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .targetAmount(goal.getTargetAmount())
                .savedAmount(goal.getSavedAmount())
                .deadline(goal.getDeadline())
                .completionPercentage(Math.min(percentage, 100))
                .build();
    }
}
