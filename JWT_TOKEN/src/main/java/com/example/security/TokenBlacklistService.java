package com.example.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    // In-memory blacklist (consider Redis for production)
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    // Add token JTI to blacklist with expiration time
    public void addToBlacklist(String jti, long expiresInMs) {
        long expirationTime = System.currentTimeMillis() + expiresInMs;
        blacklist.put(jti, expirationTime);
        log.info("Token {} added to blacklist, expires at {}", jti, new Date(expirationTime));
    }

    // Check if token is blacklisted
    public boolean isBlacklisted(String jti) {
        Long expirationTime = blacklist.get(jti);
        
        if (expirationTime == null) {
            return false; // Not in blacklist
        }

        // Check if still valid (not expired)
        if (System.currentTimeMillis() > expirationTime) {
            blacklist.remove(jti); // Clean up expired entries
            return false;
        }

        return true; // Token is blacklisted and still active
    }

    // Revoke token by JTI
    public void revokeToken(String jti) {
        blacklist.put(jti, System.currentTimeMillis()); // Immediate revocation
        log.info("Token {} revoked immediately", jti);
    }

    // Clean up expired entries (run periodically)
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int beforeSize = blacklist.size();
        
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        
        int afterSize = blacklist.size();
        log.debug("Cleaned up {} expired tokens. Blacklist size: {} -> {}", 
                  beforeSize - afterSize, beforeSize, afterSize);
    }

    // Get blacklist size (for monitoring)
    public int getBlacklistSize() {
        return blacklist.size();
    }

    // Check token status
    public String getTokenStatus(String jti) {
        Long expirationTime = blacklist.get(jti);
        
        if (expirationTime == null) {
            return "NOT_BLACKLISTED";
        }

        if (System.currentTimeMillis() > expirationTime) {
            return "BLACKLIST_EXPIRED";
        }

        return "BLACKLISTED";
    }
}
