package com.example.controller;

import com.example.dto.*;
import com.example.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(
                    loginRequest.getUsername(),
                    loginRequest.getUsername() + "@example.com",
                    1L
            );

            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(jwt)
                    .tokenType("Bearer")
                    .expiresIn(86400000L)
                    .username(loginRequest.getUsername())
                    .email(loginRequest.getUsername() + "@example.com")
                    .build());

        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            if (!tokenProvider.validateToken(request.getToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid token", "The provided token is invalid or expired"));
            }

            String newToken = tokenProvider.refreshToken(request.getToken());

            String username = tokenProvider.extractUsername(request.getToken());
            String email = tokenProvider.extractEmail(request.getToken());

            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(newToken)
                    .tokenType("Bearer")
                    .expiresIn(86400000L)
                    .username(username)
                    .email(email)
                    .build());

        } catch (Exception e) {
            log.error("Token refresh failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed", e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody ValidateTokenRequest request) {
        try {
            Boolean isValid = tokenProvider.validateToken(request.getToken());

            if (isValid) {
                String username = tokenProvider.extractUsername(request.getToken());
                return ResponseEntity.ok(ValidateTokenResponse.builder()
                        .valid(true)
                        .message("Token is valid")
                        .username(username)
                        .build());
            } else {
                return ResponseEntity.ok(ValidateTokenResponse.builder()
                        .valid(false)
                        .message("Token is invalid or expired")
                        .build());
            }

        } catch (Exception e) {
            log.error("Token validation failed");
            return ResponseEntity.ok(ValidateTokenResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();

                return ResponseEntity.ok(UserResponse.builder()
                        .id(1L)
                        .username(username)
                        .email(username + "@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .build());
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", "No authenticated user found"));

        } catch (Exception e) {
            log.error("Failed to get current user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error", e.getMessage()));
        }
    }
}
