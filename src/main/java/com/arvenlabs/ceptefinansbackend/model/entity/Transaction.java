package com.arvenlabs.ceptefinansbackend.model.entity;

import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import com.arvenlabs.ceptefinansbackend.model.enums.TransactionSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_user_transaction_history", columnList = "user_id, transaction_date"),
        @Index(name = "idx_user_transaction_type", columnList = "user_id, type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // İşlemi yapan kullanıcı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // İşlemin kategorisi (Örn: Market)
    @ManyToOne(fetch = FetchType.EAGER) // İşlemi çekerken kategorisini de hemen getir
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // PERFORMANS ALANI: Kategori tablosuna gitmeden gelir/gider olduğunu anlamak için
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Column(nullable = false)
    private BigDecimal amount; // Para işlemleri için Double yerine BigDecimal kullanılır!

    @Column(length = 500)
    private String description;

    // Harcamanın yapıldığı tarih (Saat önemli değilse LocalDate, önemliyse LocalDateTime)
    // Finans uygulamalarında genelde fiş tarihi (LocalDate) daha önemlidir.
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionSource source = TransactionSource.MANUAL;

    // Konum bilgisi (Haritada göstermek için)
    private Double latitude;
    private Double longitude;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}