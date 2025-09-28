package com.redbus.service;

import com.redbus.model.User;
import com.redbus.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecretKey secretKey;

    public AuthService(UserRepository userRepository,
                      @Value("${jwt.secret}") String jwtSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String signup(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return generateToken(user.getUserId());
    }

    public String login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            System.out.println("User not found");
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = userOpt.get();

        System.out.println("User found: " + user);
        String passwordHash = passwordEncoder.encode(password);
        System.out.println("Password hash: " + user.getPasswordHash());
        System.out.println("Password: " + password);
        System.out.println("Password hash2: " + passwordHash);
        System.out.println("Password matches: " + passwordEncoder.matches(password, user.getPasswordHash()));


        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        System.out.println("Password matches");

        return generateToken(user.getUserId());
    }

    public boolean auth(String authToken, String userId) {
        if (authToken == null || userId == null) {
            return false;
        }
        String extractedUserId = extractUserIdFromToken(authToken);
        return extractedUserId.equals(userId);
    }

    public String extractUserId(String authHeader) {
        System.out.println("extractUserId:AuthHeader: " + authHeader);
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return extractUserIdFromToken(token);
    }

    public String extractUserIdFromToken(String token) {
        System.out.println("extractUserIdFromToken:Token: " + token);
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            System.out.println("extractUserIdFromToken:Invalid token");
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private String generateToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 24 * 60 * 60 * 1000); // 24 hours

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
}
