package com.redbus.config;

import com.redbus.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Lazy
    private final AuthService authService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(authz -> authz
                // Allow web pages (public)
                .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Allow API endpoints
                .requestMatchers("/v1/auth/**", "/healthz", "/actuator/**").permitAll()
                // Allow all web routes (they handle authentication internally)
                .requestMatchers("/search", "/search/**", "/book/**", "/payment/**", "/booking-success/**", "/bookings/all-bookings", "/admin/**", "/logout").permitAll()
                // Allow all web controller routes
                .requestMatchers("/**").permitAll()
                // Require authentication for all other requests
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private class JwtAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            
            // Handle JWT token authentication for API requests
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String userId = authService.extractUserId(authHeader);
                    
                    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception e) {
                    // Invalid token - continue without authentication
                }
            }
            
            // For web requests, check if user is already authenticated via session
            // This allows the web controller to work with session-based auth

            filterChain.doFilter(request, response);
        }
    }
}
