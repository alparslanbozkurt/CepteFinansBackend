package com.arvenlabs.ceptefinansbackend.model.entity;

import com.arvenlabs.ceptefinansbackend.model.enums.BudgetPeriod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // Hangi kullanıcıya ait?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Hangi kategori için bütçe koydu? (Örn: Market)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Limit ne kadar? (Örn: 5000 TL)
    @Column(nullable = false)
    private BigDecimal amount;

    // Dönem (Aylık, Haftalık)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    // Bütçe ne zaman başlıyor?
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Bütçe ne zaman bitiyor? (Otomatik hesaplanacak)
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_seventy_percent_notified")
    @Builder.Default
    private boolean isSeventyPercentNotified = false;

    // %100 (Bütçe aşıldı) uyarısı gönderildi mi?
    @Column(name = "is_hundred_percent_notified")
    @Builder.Default
    private boolean isHundredPercentNotified = false;
}