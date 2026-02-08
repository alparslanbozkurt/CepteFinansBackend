package com.arvenlabs.ceptefinansbackend.config;

import com.arvenlabs.ceptefinansbackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. İsteğin başlığından (Header) "Authorization" kısmını al
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Kontrol: Header boş mu veya "Bearer " ile başlamıyor mu?
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Token yoksa filtre zincirine devam et (belki herkese açık bir sayfadır)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " kısmını atıp sadece token'ı al (7 karakter sonrasını al)
        jwt = authHeader.substring(7);

        // 4. Token içinden e-postayı çıkar
        userEmail = jwtService.extractUsername(jwt);

        // 5. E-posta var mı ve kullanıcı şu an sistemde "Authenticated" değil mi?
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Veritabanından kullanıcıyı getir
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Token geçerli mi diye sor
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Geçerliyse, Spring Security'e "Bu adam bizden, içeri al" diyoruz
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Güvenlik Bağlamına (Context) kullanıcıyı kaydet
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 7. Zincire devam et
        filterChain.doFilter(request, response);
    }
}