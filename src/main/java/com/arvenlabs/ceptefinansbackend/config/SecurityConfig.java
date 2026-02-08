package com.arvenlabs.ceptefinansbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Final olarak tanımlıyoruz ki Lombok constructor'da bunları otomatik doldursun
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF Korumasını Devre Dışı Bırak (REST API olduğu için gerek yok)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS Ayarlarını Aktif Et
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Yetkilendirme Kuralları (EN ÖNEMLİ KISIM)
                .authorizeHttpRequests(auth -> auth
                        // Giriş yapma ve Kayıt olma herkese açık olmalı
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Test endpointi açık kalsın (İsteğe bağlı)
                        .requestMatchers("/api/v1/infra-test/**").permitAll()

                        // Geri kalan TÜM yollar için token (giriş) zorunlu!
                        .anyRequest().authenticated()
                )

                // 4. Oturum Yönetimi (Stateless)
                // Sunucuda session tutma, her istekte token bekle diyoruz.
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. Bizim yazdığımız AuthenticationProvider'ı tanımla
                .authenticationProvider(authenticationProvider)

                // 6. Filtremizi devreye al
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}