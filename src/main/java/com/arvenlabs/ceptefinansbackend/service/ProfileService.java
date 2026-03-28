package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.ProfileUpdateRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ProfileResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileResponse getProfile() {
        User user = getCurrentUser();
        return mapToResponse(user);
    }

    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = getCurrentUser();

        // Sadece gelen (null olmayan) alanları güncelle
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getIncome() != null) {
            user.setIncome(request.getIncome());
        }
        if (request.getOccupation() != null) {
            user.setOccupation(request.getOccupation());
        }

        userRepository.save(user);
        return mapToResponse(user);
    }

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    private ProfileResponse mapToResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .age(user.getAge())
                .income(user.getIncome())
                .occupation(user.getOccupation())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
