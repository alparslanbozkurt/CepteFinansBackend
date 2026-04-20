package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.UserLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLayoutRepository extends JpaRepository<UserLayout, UUID> {
    Optional<UserLayout> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
