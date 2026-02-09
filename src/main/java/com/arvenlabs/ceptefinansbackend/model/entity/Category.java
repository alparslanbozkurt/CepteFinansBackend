package com.arvenlabs.ceptefinansbackend.model.entity;

import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 1, 2, 3 diye artar
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type; // GELİR mi GİDER mi?

    @Column(name = "icon")
    private String icon; // Örn: "fa-home", "fa-wallet" (Frontend ikon seti için)
}