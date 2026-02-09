package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Aynı isimde kategori var mı diye kontrol etmek için
    Optional<Category> findByName(String name);
}