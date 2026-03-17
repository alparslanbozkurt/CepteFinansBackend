package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.VaultRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.VaultResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.Category;
import com.arvenlabs.ceptefinansbackend.model.entity.ImpulseLock;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.LockStatus;
import com.arvenlabs.ceptefinansbackend.repository.CategoryRepository;
import com.arvenlabs.ceptefinansbackend.repository.ImpulseLockRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final ImpulseLockRepository impulseLockRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    @Transactional
    public VaultResponse lockCategory(VaultRequest request) {
        User user = getCurrentUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kullanılabilir kategori bulunamadı!"));

        // Check if there's already an active lock for this category
        impulseLockRepository.findByUserIdAndCategoryIdAndStatus(user.getId(), category.getId(), LockStatus.ACTIVE)
                .ifPresent(lock -> {
                    throw new RuntimeException("Bu kategori için zaten aktif bir kilidiniz var.");
                });

        LocalDateTime now = LocalDateTime.now();

        ImpulseLock impulseLock = ImpulseLock.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .lockedAt(now)
                .unlocksAt(now.plusHours(48)) // 48 hours lock
                .status(LockStatus.ACTIVE)
                .build();

        ImpulseLock saved = impulseLockRepository.save(impulseLock);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VaultResponse> getMyLocks() {
        User user = getCurrentUser();
        // Return active or properly mapped locks
        return impulseLockRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VaultResponse mapToResponse(ImpulseLock lock) {
        // If an active lock's time has expired, it should be considered UNLOCKED
        // A scheduled job could do this, but checking on fetch is safer.
        LockStatus returnStatus = lock.getStatus();
        if (returnStatus == LockStatus.ACTIVE && LocalDateTime.now().isAfter(lock.getUnlocksAt())) {
            returnStatus = LockStatus.UNLOCKED;
        }

        return VaultResponse.builder()
                .id(lock.getId())
                .categoryName(lock.getCategory().getName())
                .amount(lock.getAmount())
                .status(returnStatus)
                .lockedAt(lock.getLockedAt())
                .unlocksAt(lock.getUnlocksAt())
                .build();
    }
}
