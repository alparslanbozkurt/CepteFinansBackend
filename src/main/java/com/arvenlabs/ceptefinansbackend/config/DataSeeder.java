package com.arvenlabs.ceptefinansbackend.config;

import com.arvenlabs.ceptefinansbackend.model.entity.Category;
import com.arvenlabs.ceptefinansbackend.model.enums.CategoryType;
import com.arvenlabs.ceptefinansbackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        // Eğer hiç kategori yoksa standartları ekle
        if (categoryRepository.count() == 0) {
            loadCategories();
        }
    }

    private void loadCategories() {
        // --- GELİR KATEGORİLERİ ---
        List<String> incomes = Arrays.asList(
                "Maaş", "Freelance", "Yatırım", "Ek Gelir", "Kira Geliri"
        );

        // --- GİDER KATEGORİLERİ ---
        List<String> expenses = Arrays.asList(
                "Market", "Kira", "Faturalar", "Ulaşım",
                "Eğlence", "Sağlık", "Giyim", "Eğitim",
                "Teknoloji", "Tatil", "Restoran"
        );

        // Kaydetme İşlemi
        for (String name : incomes) {
            saveCategory(name, CategoryType.INCOME);
        }

        for (String name : expenses) {
            saveCategory(name, CategoryType.EXPENSE);
        }

        System.out.println("✅ Varsayılan kategoriler veritabanına yüklendi!");
    }

    private void saveCategory(String name, CategoryType type) {
        Category category = Category.builder()
                .name(name)
                .type(type)
                .icon("default-icon") // Şimdilik default, sonra icon seti ekleriz
                .build();
        categoryRepository.save(category);
    }
}