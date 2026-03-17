package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.LayoutRequest;
import com.arvenlabs.ceptefinansbackend.dto.response.LayoutResponse;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.entity.UserLayout;
import com.arvenlabs.ceptefinansbackend.repository.UserLayoutRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class LayoutService {

    private final UserLayoutRepository layoutRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
    }

    @Transactional
    public LayoutResponse saveLayout(LayoutRequest request) {
        User user = getCurrentUser();

        UserLayout layout = layoutRepository.findByUserId(user.getId()).orElse(
                UserLayout.builder()
                        .user(user)
                        .widgets(new ArrayList<>())
                        .build()
        );

        layout.setWidgets(request.getWidgets());
        UserLayout saved = layoutRepository.save(layout);

        return LayoutResponse.builder()
                .id(saved.getId())
                .widgets(saved.getWidgets())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public LayoutResponse getLayout() {
        User user = getCurrentUser();

        UserLayout layout = layoutRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı için bir layout (dizilim) bulunamadı. Lütfen önce kaydedin."));

        return LayoutResponse.builder()
                .id(layout.getId())
                .widgets(layout.getWidgets())
                .updatedAt(layout.getUpdatedAt())
                .build();
    }
}
