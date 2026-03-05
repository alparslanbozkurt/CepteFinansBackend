package com.arvenlabs.ceptefinansbackend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    // Kendi projenizdeki User entity'nize göre burayı ayarlayabilirsiniz.
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // Token'ın süresinin dolup dolmadığını kontrol eden yardımcı bir metot
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}