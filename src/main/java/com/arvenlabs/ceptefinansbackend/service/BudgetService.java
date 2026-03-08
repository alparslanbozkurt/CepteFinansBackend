package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.BudgetRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.BudgetResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Budget;
import com.arvenlabs.ceptefinansbackend.model.entity.Category;
import com.arvenlabs.ceptefinansbackend.model.entity.Transaction;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.BudgetPeriod;
import com.arvenlabs.ceptefinansbackend.repository.BudgetRepository;
import com.arvenlabs.ceptefinansbackend.repository.CategoryRepository;
import com.arvenlabs.ceptefinansbackend.repository.TransactionRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // --- 1. BÜTÇE OLUŞTUR ---
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = getCurrentUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

        // Tarihleri Hesapla (Bu ayın başı ve sonu)
        BudgetPeriod period = request.getPeriod() != null ? request.getPeriod() : BudgetPeriod.MONTHLY;
        LocalDate startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        // Çakışma Kontrolü: Aynı kategori ve dönemde bütçe var mı?
        if (budgetRepository.findOverlappingBudget(user.getId(), category.getId(), startDate, endDate).isPresent()) {
            throw new RuntimeException("Bu kategori için bu dönemde zaten bir bütçe var!");
        }

        // Kayıt
        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .period(period)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Budget saved = budgetRepository.save(budget);
        return mapToResponse(saved);
    }

    // --- 2. BÜTÇELERİ LİSTELE VE DURUMUNU HESAPLA ---
    public List<BudgetResponse> getBudgets() {
        User user = getCurrentUser();

        List<Budget> budgets = budgetRepository.findAllByUserId(user.getId());

        // Her bir bütçe için "Ne kadar harcandı?" hesaplaması yapıp DTO'ya çeviriyoruz
        return budgets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- 3. BÜTÇE SİL ---
    public void deleteBudget(UUID id) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bütçe bulunamadı"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Yetkisiz işlem");
        }
        budgetRepository.delete(budget);
    }

    // --- 4. BÜTÇE GÜNCELLE ---
    @Transactional
    public BudgetResponse updateBudget(UUID id, BudgetRequest request) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bütçe bulunamadı"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu bütçeyi düzenleme yetkiniz yok!");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

        budget.setCategory(category);
        budget.setAmount(request.getAmount());
        if (request.getPeriod() != null) {
            budget.setPeriod(request.getPeriod());
        }

        Budget updated = budgetRepository.save(budget);
        return mapToResponse(updated);
    }

    // --- 5. TEKİL BÜTÇE GETİR ---
    public BudgetResponse getBudgetById(UUID id) {
        User user = getCurrentUser();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bütçe bulunamadı"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu bütçeye erişim yetkiniz yok!");
        }
        return mapToResponse(budget);
    }

    // --- YARDIMCI METOD: Entity -> DTO (Hesaplamalar Burada!) ---
    private BudgetResponse mapToResponse(Budget budget) {
        // 1. Bu bütçenin kategorisinde ve tarih aralığında yapılan harcamaları bul
        List<Transaction> transactions = transactionRepository
                .findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        budget.getUser().getId(),
                        budget.getStartDate(),
                        budget.getEndDate());

        // 2. Sadece o kategoriye ait olanları ve GİDER olanları topla
        BigDecimal spentAmount = transactions.stream()
                .filter(t -> t.getCategory().getId().equals(budget.getCategory().getId()))
                .filter(t -> t.getType().name().equals("EXPENSE")) // Sadece giderler
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Yüzdeyi Hesapla (spent / limit * 100)
        double percentage = 0.0;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = spentAmount.divide(budget.getAmount(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryName(budget.getCategory().getName())
                .limitAmount(budget.getAmount())
                .spentAmount(spentAmount) // Hesaplanan harcama
                .percentage(percentage) // Hesaplanan yüzde
                .period(budget.getPeriod())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .build();
    }

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }
}