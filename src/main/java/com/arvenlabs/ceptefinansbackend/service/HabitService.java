package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.model.entity.Transaction;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import com.arvenlabs.ceptefinansbackend.repository.TransactionRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    /**
     * Creates a boolean array representing the last 30 days.
     * True means "Good Habit Day" -> 0 spending.
     * False means a regular or impulsive pending day.
     */
    public List<Boolean> getStreakMap() {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29); // include today = 30 days total

        List<Transaction> expenses = transactionRepository
                .findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(user.getId(), startDate, today)
                .stream()
                .filter(t -> t.getType() == CategoryType.EXPENSE)
                .toList();

        Map<LocalDate, Double> dailyExpenses = expenses.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getTransactionDate,
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())
                ));

        List<Boolean> streakMap = new ArrayList<>();
        // Iterate over the last 30 days chronologically
        for (int i = 0; i <= 29; i++) {
            LocalDate dateToCheck = startDate.plusDays(i);
            Double spentOnDay = dailyExpenses.getOrDefault(dateToCheck, 0.0);
            
            // Streak condition: Spent nothing
            boolean isGoodDay = spentOnDay == 0.0;
            streakMap.add(isGoodDay);
        }

        return streakMap;
    }
}
