package com.example.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Slf4j
@Component
public class AdvancedJwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.expiration:900000}") // 15 minutes
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration:604800000}") // 7 days
    private long refreshTokenExpiration;

    @Value("${jwt.issuer:https://auth.example.com}")
    private String issuer;

    @Value("${jwt.audience:https://api.example.com}")
    private String audience;

    // Access Token Generation
    public String generateAccessToken(String userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("username", username);
        claims.put("type", "ACCESS");
        
        return createToken(claims, userId, accessTokenExpiration);
    }

    // Refresh Token Generation
    public String generateRefreshToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("type", "REFRESH");
        claims.put("tokenVersion", 1); // For token rotation
        
        return createToken(claims, userId, refreshTokenExpiration);
    }

    // ID Token Generation (for OAuth 2.0)
    public String generateIdToken(String userId, String username, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("email", email);
        claims.put("type", "ID");
        
        return createToken(claims, userId, 3600000); // 1 hour
    }

    // Create JWT Token with advanced claims
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        String jwtId = UUID.randomUUID().toString(); // JTI for revocation

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setId(jwtId) // JWT ID for tracking and revocation
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer) // Token issuer
                .setAudience(audience) // Token audience
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token with comprehensive checks
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

    // Extract all claims
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract specific claims
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object rolesObj = claims.get("roles");
        
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return new ArrayList<>();
    }

    public String extractTokenType(String token) {
        return (String) extractAllClaims(token).get("type");
    }

    public Long extractExpiresIn(String token) {
        return extractAllClaims(token).getExpiration().getTime() - new Date().getTime();
    }

    public Boolean isTokenExpired(String token) {
        try {
            extractAllClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Boolean isTokenOfType(String token, String expectedType) {
        String tokenType = extractTokenType(token);
        return expectedType.equals(tokenType);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
