package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.ProfileUpdateRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.ProfileResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

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

    public void uploadProfileImage(MultipartFile file) throws IOException {
        User user = getCurrentUser();
        user.setProfileImage(file.getBytes());
        user.setProfileImageContentType(file.getContentType());
        userRepository.save(user);
    }

    private User getCurrentUser() {
        return (com.arvenlabs.ceptefinansbackend.model.entity.User) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ProfileResponse mapToResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .age(user.getAge())
                .income(user.getIncome())
                .occupation(user.getOccupation())
                .profileImageUrl(user.getProfileImage() != null ? "/api/v1/profile/image/" + user.getId() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
