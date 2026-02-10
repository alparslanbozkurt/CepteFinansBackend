package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    // Kullanıcının tüm bütçelerini getir
    List<Budget> findAllByUserId(UUID userId);

    // Aynı kategori ve tarih aralığında çakışan bütçe var mı?
    // Örn: Ocak ayı için "Market" bütçesi varken, tekrar Ocak ayına "Market" bütçesi eklenemesin.
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
            "AND b.category.id = :categoryId " +
            "AND ((b.startDate BETWEEN :start AND :end) OR (b.endDate BETWEEN :start AND :end))")
    Optional<Budget> findOverlappingBudget(
            @Param("userId") UUID userId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}