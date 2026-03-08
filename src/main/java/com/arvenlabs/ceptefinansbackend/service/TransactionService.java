package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.TransactionRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.TransactionResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Category;
import com.arvenlabs.ceptefinansbackend.model.entity.Transaction;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.TransactionSource;
import com.arvenlabs.ceptefinansbackend.repository.BudgetRepository;
import com.arvenlabs.ceptefinansbackend.repository.CategoryRepository;
import com.arvenlabs.ceptefinansbackend.repository.TransactionRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BudgetRepository budgetRepository;

    // --- 1. HARCAMA EKLE (CREATE) ---
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        // Şu anki kullanıcıyı bul
        User user = getCurrentUser();

        // Kategoriyi bul
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı!"));

        // VALIDATION: Kategori tipi ile İşlem tipi uyuşuyor mu?
        if (category.getType() != request.getType()) {
            throw new RuntimeException("Seçilen kategori bu işlem türü için uygun değil! " +
                    "(Örn: Gelir kategorisine Gider yazılamaz)");
        }

        // Kayıt Oluştur
        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .source(request.getSource() != null ? request.getSource() : TransactionSource.MANUAL)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // EĞER GİDER İSE BÜTÇE KONTROLÜ YAP
        if (saved.getType().name().equals("EXPENSE")) {
            checkBudgetLimits(user, category, saved.getTransactionDate());
        }

        return mapToResponse(saved);
    }

    // --- 2. KULLANICININ TÜM İŞLEMLERİNİ GETİR (READ ALL) ---
    public List<TransactionResponse> getAllTransactions() {
        User user = getCurrentUser();
        return transactionRepository.findAllByUserIdOrderByTransactionDateDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- 3. TARİH ARALIĞINA GÖRE FİLTRELE (FILTER) ---
    public List<TransactionResponse> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        User user = getCurrentUser();
        // Repository'deki özel sorgu metodunu çağırıyoruz
        return transactionRepository.findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                user.getId(), startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- 4. GÜNCELLEME (UPDATE) ---
    @Transactional
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request) {
        User user = getCurrentUser();

        // İşlemi bul
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İşlem bulunamadı"));

        // Güvenlik Kontrolü: İşlem bu kullanıcıya mı ait?
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu işlemi düzenleme yetkiniz yok!");
        }

        // Kategori değişikliği varsa kontrol et
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Kategori bulunamadı"));

            // Validation: Tip uyuşmazlığı kontrolü
            if (category.getType() != request.getType()) {
                throw new RuntimeException("Kategori tipi ile işlem tipi uyuşmuyor!");
            }
            transaction.setCategory(category);
        }

        // Diğer alanları güncelle
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setType(request.getType());

        // Veritabanına kaydet
        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    // --- 5. SİLME İŞLEMİ (DELETE) ---
    @Transactional
    public void deleteTransaction(UUID id) {
        User user = getCurrentUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İşlem bulunamadı"));

        // Güvenlik Kontrolü
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu işlemi silme yetkiniz yok!");
        }

        transactionRepository.delete(transaction);
    }

    // --- 6. TEKİL GETİR (READ ONE) ---
    public TransactionResponse getTransactionById(UUID id) {
        User user = getCurrentUser();
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("İşlem bulunamadı"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bu işleme erişim yetkiniz yok!");
        }
        return mapToResponse(transaction);
    }

    // --- YARDIMCI METODLAR ---

    // Entity -> DTO Dönüşümü
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .categoryName(transaction.getCategory().getName())
                .categoryIcon(transaction.getCategory().getIcon())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }

    // Şu an login olmuş kullanıcıyı getiren metod
    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    // Bütçe Kontrolü ve Bildirim Gönderimi
    private void checkBudgetLimits(User user, Category category, LocalDate transactionDate) {
        // 1. Bu tarihe uyan aktif bir bütçe var mı?
        LocalDate startDate = transactionDate.withDayOfMonth(1);
        LocalDate endDate = transactionDate.withDayOfMonth(transactionDate.lengthOfMonth());

        budgetRepository.findOverlappingBudget(user.getId(), category.getId(), startDate, endDate)
                .ifPresent(budget -> {
                    // 2. Bu ayki toplam harcamayı çek
                    Double totalSpent = transactionRepository.findTotalExpenseByCategoryAndDate(
                            user.getId(), category.getId(), budget.getStartDate(), budget.getEndDate());

                    if (totalSpent == null)
                        totalSpent = 0.0;

                    double percentage = (totalSpent / budget.getAmount().doubleValue()) * 100;

                    // 3. %100 Kontrolü (HATA BURADA DÜZELTİLDİ: createAndSendNotification
                    // kullanıldı)
                    if (percentage >= 100.0 && !budget.isHundredPercentNotified()) {
                        notificationService.createAndSendNotification(user,
                                "🚨 Bütçe Aşıldı!",
                                category.getName() + " kategorisi için belirlediğin bütçeyi aştın!");

                        budget.setHundredPercentNotified(true);
                        budget.setSeventyPercentNotified(true);
                        budgetRepository.save(budget);
                    }
                    // 4. %70 Kontrolü (HATA BURADA DÜZELTİLDİ: createAndSendNotification
                    // kullanıldı)
                    else if (percentage >= 70.0 && !budget.isSeventyPercentNotified()) {
                        notificationService.createAndSendNotification(user,
                                "⚠️ Bütçe Uyarısı",
                                category.getName() + " bütçenin %70'ini doldurdun. Dikkatli harca!");

                        budget.setSeventyPercentNotified(true);
                        budgetRepository.save(budget);
                    }
                });
    }
}