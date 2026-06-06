package com.example.controller;

import com.example.dto.*;
import com.example.security.AdvancedJwtTokenProvider;
import com.example.security.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/advanced")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdvancedAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AdvancedJwtTokenProvider advancedTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserDetailsService userDetailsService;

    // Advanced login with access + refresh tokens
    @PostMapping("/login-advanced")
    public ResponseEntity<?> advancedLogin(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            String userId = "1"; // In real app, get from user repository
            List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");

            String accessToken = advancedTokenProvider.generateAccessToken(
                    userId,
                    loginRequest.getUsername(),
                    roles
            );

            String refreshToken = advancedTokenProvider.generateRefreshToken(
                    userId,
                    loginRequest.getUsername()
            );

            String jti = advancedTokenProvider.extractJti(accessToken);

            return ResponseEntity.ok(AdvancedTokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900000L) // 15 minutes
                    .refreshExpiresIn(604800000L) // 7 days
                    .username(loginRequest.getUsername())
                    .email(loginRequest.getUsername() + "@example.com")
                    .roles(roles)
                    .jti(jti)
                    .build());

        } catch (Exception e) {
            log.error("Advanced login failed for user: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed", e.getMessage()));
        }
    }

    // Refresh tokens with rotation
    @PostMapping("/token-refresh")
    public ResponseEntity<?> refreshTokenAdvanced(@RequestBody RefreshTokenRequest request) {
        try {
            if (!advancedTokenProvider.validateToken(request.getToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid token", "Refresh token is invalid or expired"));
            }

            // Verify it's actually a refresh token
            if (!advancedTokenProvider.isTokenOfType(request.getToken(), "REFRESH")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid token type", "Token is not a refresh token"));
            }

            String userId = advancedTokenProvider.extractUsername(request.getToken());
            // username is stored as a claim in the refresh token
            String username = (String) advancedTokenProvider.extractAllClaims(request.getToken()).get("username");

            // Prefer server-side lookup for current roles so role changes are respected
            List<String> roles;
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Fallback: if roles aren't found in user store, try extracting from token (if present)
                roles = advancedTokenProvider.extractRoles(request.getToken());
            }

            // Generate new tokens
            String newAccessToken = advancedTokenProvider.generateAccessToken(userId, username, roles);
            String newRefreshToken = advancedTokenProvider.generateRefreshToken(userId, username);

            // Optionally: Revoke old refresh token
            String oldJti = advancedTokenProvider.extractJti(request.getToken());
            tokenBlacklistService.revokeToken(oldJti);
            log.info("Old refresh token revoked: {}", oldJti);

            return ResponseEntity.ok(AdvancedTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900000L)
                    .refreshExpiresIn(604800000L)
                    .username(username)
                    .email(username + "@example.com")
                    .roles(roles)
                    .jti(advancedTokenProvider.extractJti(newAccessToken))
                    .build());

        } catch (Exception e) {
            log.error("Token refresh failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed", e.getMessage()));
        }
    }

    // Advanced token validation with detailed info
    @PostMapping("/validate-advanced")
    public ResponseEntity<?> validateTokenAdvanced(@RequestBody ValidateTokenRequest request) {
        try {
            if (!advancedTokenProvider.validateToken(request.getToken())) {
                return ResponseEntity.ok(AdvancedValidateTokenResponse.builder()
                        .valid(false)
                        .message("Token is invalid or expired")
                        .build());
            }

            String username = advancedTokenProvider.extractUsername(request.getToken());
            String jti = advancedTokenProvider.extractJti(request.getToken());
            String tokenType = advancedTokenProvider.extractTokenType(request.getToken());
            Long expiresIn = advancedTokenProvider.extractExpiresIn(request.getToken());

            return ResponseEntity.ok(AdvancedValidateTokenResponse.builder()
                    .valid(true)
                    .message("Token is valid")
                    .username(username)
                    .jti(jti)
                    .tokenType(tokenType)
                    .expiresIn(expiresIn)
                    .build());

        } catch (Exception e) {
            log.error("Token validation failed");
            return ResponseEntity.ok(AdvancedValidateTokenResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + e.getMessage())
                    .build());
        }
    }

    // Logout - Blacklist tokens
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer "
            String jti = advancedTokenProvider.extractJti(token);
            Long expiresIn = advancedTokenProvider.extractExpiresIn(token);

            // Add to blacklist
            tokenBlacklistService.addToBlacklist(jti, expiresIn);

            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(new ErrorResponse("Success", "Logged out successfully"));

        } catch (Exception e) {
            log.error("Logout failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Logout failed", e.getMessage()));
        }
    }

    // Check if token is blacklisted (accept either full JWT or JTI string)
    @PostMapping("/check-blacklist")
    public ResponseEntity<?> checkBlacklist(@RequestBody ValidateTokenRequest request) {
        try {
            String tokenOrJti = request.getToken();
            if (tokenOrJti == null || tokenOrJti.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request", "Token or JTI is required"));
            }

            String jti;
            // If the input looks like a JWT (contains exactly two dots), extract jti from it
            if (tokenOrJti.chars().filter(ch -> ch == '.').count() == 2) {
                jti = advancedTokenProvider.extractJti(tokenOrJti);
            } else {
                // treat the provided value as JTI directly
                jti = tokenOrJti;
            }

            String status = tokenBlacklistService.getTokenStatus(jti);
            return ResponseEntity.ok(new ErrorResponse("Status", status));

        } catch (Exception e) {
            log.error("Blacklist check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Check failed", e.getMessage()));
        }
    }

    // Get token information
    @PostMapping("/token-info")
    public ResponseEntity<?> getTokenInfo(@RequestBody ValidateTokenRequest request) {
        try {
            if (!advancedTokenProvider.validateToken(request.getToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid token", "Token is invalid or expired"));
            }

            String username = advancedTokenProvider.extractUsername(request.getToken());
            String jti = advancedTokenProvider.extractJti(request.getToken());
            String tokenType = advancedTokenProvider.extractTokenType(request.getToken());
            List<String> roles = advancedTokenProvider.extractRoles(request.getToken());
            Long expiresIn = advancedTokenProvider.extractExpiresIn(request.getToken());
            boolean isExpired = advancedTokenProvider.isTokenExpired(request.getToken());

            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "jti", jti,
                    "tokenType", tokenType,
                    "roles", roles,
                    "expiresIn", expiresIn,
                    "isExpired", isExpired
            ));

        } catch (Exception e) {
            log.error("Token info retrieval failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error", e.getMessage()));
        }
    }

    // Get blacklist statistics
    @GetMapping("/blacklist-stats")
    public ResponseEntity<?> getBlacklistStats() {
        try {
            int blacklistSize = tokenBlacklistService.getBlacklistSize();
            return ResponseEntity.ok(Map.of(
                    "blacklistSize", blacklistSize,
                    "message", "Current blacklisted tokens"
            ));

        } catch (Exception e) {
            log.error("Blacklist stats retrieval failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error", e.getMessage()));
        }
    }

    // Clean up expired tokens (admin endpoint)
    @PostMapping("/cleanup-blacklist")
    public ResponseEntity<?> cleanupBlacklist() {
        try {
            int beforeSize = tokenBlacklistService.getBlacklistSize();
            tokenBlacklistService.cleanupExpiredTokens();
            int afterSize = tokenBlacklistService.getBlacklistSize();

            return ResponseEntity.ok(Map.of(
                    "beforeSize", beforeSize,
                    "afterSize", afterSize,
                    "cleaned", beforeSize - afterSize
            ));

        } catch (Exception e) {
            log.error("Blacklist cleanup failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error", e.getMessage()));
        }
    }
}
