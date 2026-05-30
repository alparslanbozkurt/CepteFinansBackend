package com.arvenlabs.ceptefinansbackend.service;

import com.arvenlabs.ceptefinansbackend.dto.request.VerifyEmailRequest;
import com.arvenlabs.ceptefinansbackend.exception.BadRequestException;
import com.arvenlabs.ceptefinansbackend.model.entity.SecurityCode;
import com.arvenlabs.ceptefinansbackend.model.entity.User;
import com.arvenlabs.ceptefinansbackend.model.enums.SecurityCodeType;
import com.arvenlabs.ceptefinansbackend.repository.SecurityCodeRepository;
import com.arvenlabs.ceptefinansbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityCodeRepository securityCodeRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SecurityCode testSecurityCode;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .isEmailVerified(false)
                .build();

        testSecurityCode = SecurityCode.builder()
                .user(testUser)
                .code("123456")
                .type(SecurityCodeType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();
    }

    @Test
    void verifyEmail_InvalidCode_ShouldThrowBadRequestException() {
        // Arrange
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("654321"); // Wrong code

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(securityCodeRepository.findFirstByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(testUser, SecurityCodeType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(testSecurityCode));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.verifyEmail(request);
        });

        assertEquals("Doğrulama kodu hatalı, lütfen tekrar deneyin.", exception.getMessage());
    }

    @Test
    void verifyEmail_ExpiredCode_ShouldThrowBadRequestException() {
        // Arrange
        testSecurityCode.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(securityCodeRepository.findFirstByUserAndTypeAndIsUsedFalseOrderByCreatedAtDesc(testUser, SecurityCodeType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(testSecurityCode));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.verifyEmail(request);
        });

        assertEquals("Bu kodun süresi dolmuş (5 dakika). Lütfen yeni bir kod isteyin.", exception.getMessage());
    }

    @Test
    void verifyEmail_UserAlreadyVerified_ShouldThrowBadRequestException() {
        // Arrange
        testUser.setEmailVerified(true);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authService.verifyEmail(request);
        });

        assertEquals("E-posta adresiniz zaten onaylanmış. Giriş yapabilirsiniz.", exception.getMessage());
    }
}
