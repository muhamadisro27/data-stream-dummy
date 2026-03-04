package com.streaming.features.auth;

import com.streaming.features.auth.models.LoginDto;
import com.streaming.features.auth.models.LoginResponseDto;
import com.streaming.features.auth.models.UserInfoDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final String DEMO_EMAIL = "admin@example.com";
    private static final String DEMO_PASSWORD = "password123";
    private static final String DEMO_NAME = "Demo Admin";

    @Value("${JWT_SECRET:supersecretkeythatisatleast32byteslong}")
    private String jwtSecret;

    public LoginResponseDto login(LoginDto loginDto) {
        if (DEMO_EMAIL.equals(loginDto.getEmail()) && DEMO_PASSWORD.equals(loginDto.getPassword())) {
            UserInfoDto user = new UserInfoDto(DEMO_EMAIL, DEMO_NAME);
            String accessToken = generateToken(DEMO_EMAIL);
            return new LoginResponseDto(accessToken, user);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    private String generateToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("email", email)
                // NestJS wasn't explicitly setting expiration in the snippet, we'll leave it without or can add it if needed.
                .signWith(key)
                .compact();
    }
}
