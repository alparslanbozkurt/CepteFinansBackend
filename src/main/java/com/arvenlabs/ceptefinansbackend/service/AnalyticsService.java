package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.response.analytics.OrbitResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Budget;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.BudgetRepository;
import com.arvenlabs.ceptefinansbackend.repository.TransactionRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    public List<OrbitResponse> getWealthOrbit() {
        User user = getCurrentUser();
        LocalDate now = LocalDate.now();

        // Get active budgets for the current user
        List<Budget> activeBudgets = budgetRepository.findAllByUserId(user.getId()).stream()
                .filter(b -> !now.isBefore(b.getStartDate()) && !now.isAfter(b.getEndDate()))
                .toList();

        return activeBudgets.stream().map(budget -> {
            Double totalSpentDouble = transactionRepository.findTotalExpenseByCategoryAndDate(
                    user.getId(), budget.getCategory().getId(), budget.getStartDate(), budget.getEndDate());

            BigDecimal currentAmount = totalSpentDouble != null ? BigDecimal.valueOf(totalSpentDouble) : BigDecimal.ZERO;
            BigDecimal limit = budget.getAmount();

            double percentage = 0.0;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                percentage = currentAmount.doubleValue() / limit.doubleValue();
            }

            // Temel Gezegen Hızı: Harcama azken yavaş (10000ms), harcama çokken hızlı (1000ms)
            long orbitSpeedMs = 10000L - (long) (Math.min(percentage, 1.0) * 9000L);
            boolean isVibrating = percentage >= 1.0;

            return OrbitResponse.builder()
                    .planetName(budget.getCategory().getName())
                    .currentAmount(currentAmount)
                    .limit(limit)
                    .orbitSpeedMs(orbitSpeedMs)
                    .isVibrating(isVibrating)
                    .build();
        }).collect(Collectors.toList());
    }
}
