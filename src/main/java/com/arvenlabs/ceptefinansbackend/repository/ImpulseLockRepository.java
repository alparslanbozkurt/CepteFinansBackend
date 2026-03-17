package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.ImpulseLock;
import com.arvenlabs.ceptefinansbackend.model.enums.LockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImpulseLockRepository extends JpaRepository<ImpulseLock, UUID> {
    
    // Find active lock by user and category
    Optional<ImpulseLock> findByUserIdAndCategoryIdAndStatus(UUID userId, Long categoryId, LockStatus status);
    
    // List all locks for user
    List<ImpulseLock> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
