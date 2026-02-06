package com.arvenlabs.ceptefinansbackend.exception;

import com.arvenlabs.ceptefinansbackend.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j // Logger
public class GlobalExceptionHandler {

    // Genel Hatalar
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleAllExceptions(Exception ex) {
        log.error("Beklenmedik Hata: ", ex);
        return new ResponseEntity<>(
                ApiResponse.error("Sunucu tarafında bir hata oluştu: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Validation Hataları (@NotNull, @Email vb. takılınca burası çalışır)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return new ResponseEntity<>(
                ApiResponse.success(errors, "Veri doğrulama hatası"),
                HttpStatus.BAD_REQUEST
        );
    }
}