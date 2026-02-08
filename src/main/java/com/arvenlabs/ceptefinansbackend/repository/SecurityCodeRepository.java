package com.arvenlabs.ceptefinansbackend.repository;

import com.arvenlabs.ceptefinansbackend.model.entity.SecurityCode;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.SecurityCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SecurityCodeRepository extends JpaRepository<SecurityCode, UUID> {
    // Kullanıcıya ait, kullanılmamış ve belirli tipteki son kodu bul
    Optional<SecurityCode> findFirstByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(User user, SecurityCodeType type);
    Optional<SecurityCode> findByCodeAndType(String code, SecurityCodeType type);
}