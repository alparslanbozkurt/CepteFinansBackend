package com.arvenlabs.ceptefinansbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF'i kapatıyoruz (REST API olduğu için state tutmuyoruz)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS ayarlarını aktif ediyoruz
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Şimdilik tüm isteklere izin veriyoruz (Test için)
                // Modül 1'de burayı JWT ile kilitleyeceğiz.
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        // Not: Spring Security varsayılan olarak "Helmet" benzeri
        // güvenlik headerlarını (X-Frame, X-XSS vb.) otomatik ekler.

        return http.build();
    }

    // CORS Konfigürasyonu (Frontend ve Mobil'in erişmesi için)
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080")); // İzin verilen domainler
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}