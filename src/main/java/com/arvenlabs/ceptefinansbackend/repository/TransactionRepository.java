package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Tarih aralığına göre getir (Filtreleme için)
    List<Transaction> findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID userId, LocalDate startDate, LocalDate endDate
    );

    // Dashboard için: Belirli bir aydaki toplam GELİR ve GİDERİ hesapla
    // Bu, Java tarafında döngü kurmaktan çok daha hızlıdır (Database tarafında toplanır).
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'INCOME'")
    Double findTotalIncome(@Param("userId") UUID userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = 'EXPENSE'")
    Double findTotalExpense(@Param("userId") UUID userId);

    List<Transaction> findAllByUserIdOrderByTransactionDateDesc(UUID userId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.category.id = :categoryId " +
            "AND t.type = 'EXPENSE' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Double findTotalExpenseByCategoryAndDate(
            @Param("userId") UUID userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}