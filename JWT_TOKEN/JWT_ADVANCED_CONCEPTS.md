# JWT Token - Advanced Concepts for Interview Preparation

This document covers advanced JWT concepts, implementation patterns, and interview questions.

---

## Table of Contents
1. [JWT Fundamentals](#jwt-fundamentals)
2. [Advanced Token Management](#advanced-token-management)
3. [Security Best Practices](#security-best-practices)
4. [Common Interview Questions](#common-interview-questions)
5. [Code Examples](#code-examples)

---

## JWT Fundamentals

### What is JWT?
JWT (JSON Web Token) is a stateless authentication mechanism using digitally signed tokens.

**Structure:** `Header.Payload.Signature`

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ
.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### JWT Components

#### 1. **Header**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```
- `alg`: Algorithm (HS256, RS256, ES256, etc.)
- `typ`: Token type (usually "JWT")

#### 2. **Payload (Claims)**
```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "email": "john@example.com",
  "iat": 1516239022,
  "exp": 1516325422,
  "roles": ["USER", "ADMIN"],
  "permissions": ["read", "write"]
}
```

**Standard Claims:**
- `iss` (issuer): Token issuer
- `sub` (subject): User identifier
- `aud` (audience): Intended recipient
- `exp` (expiration): Token expiration time
- `nbf` (not before): Token not valid before this time
- `iat` (issued at): Token creation time
- `jti` (JWT ID): Unique token identifier

**Custom Claims:** Any application-specific data

#### 3. **Signature**
```
HMACSHA256(base64(header) + "." + base64(payload), secret)
```
- Ensures token integrity
- Proves token hasn't been tampered with

---

## Advanced Token Management

### 1. **Refresh Token Strategy**

**Problem:** Access tokens expire for security. Forcing re-login is bad UX.

**Solution:** Use separate refresh tokens

```
┌─────────────────────────────────────────────┐
│ Access Token (Short-lived: 15 min)          │
│ - Quick validation                          │
│ - Used for API calls                        │
│ - Contains minimal claims                   │
└─────────────────────────────────────────────┘
         ↓ (expired)
┌─────────────────────────────────────────────┐
│ Refresh Token (Long-lived: 7 days)          │
│ - Stored securely (httpOnly cookie)         │
│ - Used to get new access token              │
│ - Contains user ID + rotation info          │
└─────────────────────────────────────────────┘
         ↓
    Generate new access token
```

**Implementation:**
```java
// At login
AccessToken at = generateAccessToken(user, 15 * 60 * 1000); // 15 min
RefreshToken rt = generateRefreshToken(user, 7 * 24 * 60 * 60 * 1000); // 7 days

// Store refresh token in database for tracking
saveRefreshTokenToDatabase(user.id, rt);

// Return both in response
response.accessToken = at;
response.refreshToken = rt; // Send as httpOnly cookie
```

### 2. **Token Blacklisting/Revocation**

**Problem:** Once JWT is issued, it's valid until expiration. Can't revoke early (logout).

**Solution:** Maintain a blacklist

```java
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
    String jwt = extractToken(token);
    
    // Add to blacklist (Redis, Database, or In-memory)
    tokenBlacklist.add(jwt);
    
    // Alternative: Store JTI (JWT ID)
    String jti = extractJti(jwt);
    blacklistService.revoke(jti);
    
    return ResponseEntity.ok("Logged out successfully");
}

// In filter: Check if token is blacklisted
if (tokenBlacklist.contains(jwt)) {
    throw new TokenRevokedException("Token has been revoked");
}
```

**Blacklist Storage Options:**
- **Redis**: Fast, TTL-based auto-cleanup
- **Database**: Persistent, queryable
- **In-memory Set**: Fast but loses on restart
- **JTI + Expiry**: Store only JTI, remove when expired

### 3. **Token Rotation**

**Problem:** Compromised tokens can be used indefinitely.

**Solution:** Rotate tokens periodically

```java
public TokenRotationResponse rotateTokens(String currentRefreshToken) {
    // Validate current refresh token
    if (!isValidRefreshToken(currentRefreshToken)) {
        throw new InvalidTokenException();
    }
    
    // Get user from token
    String userId = extractUserId(currentRefreshToken);
    
    // Generate new tokens
    String newAccessToken = generateAccessToken(userId);
    String newRefreshToken = generateRefreshToken(userId);
    
    // Revoke old refresh token
    revokeRefreshToken(currentRefreshToken);
    
    // Save new refresh token
    saveRefreshToken(userId, newRefreshToken);
    
    return new TokenRotationResponse(newAccessToken, newRefreshToken);
}
```

### 4. **Role-Based Access Control (RBAC)**

```java
@PostMapping("/admin-only")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminEndpoint() {
    return ResponseEntity.ok("Admin access granted");
}

@GetMapping("/user-data")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<?> userData() {
    return ResponseEntity.ok("User data");
}
```

**In JWT:**
```json
{
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "permissions": ["read:user", "write:user", "delete:user"]
}
```

### 5. **Permission-Based Access Control (PBAC)**

```java
@PostMapping("/update-profile")
@PreAuthorize("hasAuthority('write:profile')")
public ResponseEntity<?> updateProfile() {
    return ResponseEntity.ok("Profile updated");
}

@DeleteMapping("/user/{id}")
@PreAuthorize("hasAuthority('delete:user')")
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    return ResponseEntity.ok("User deleted");
}
```

### 6. **Audience (aud) Claim**

**Problem:** Token issued for service A shouldn't be used on service B.

**Solution:** Add `aud` claim

```java
String token = Jwts.builder()
    .setAudience("https://api.example.com") // Who this token is for
    .setSubject(userId)
    // ... other claims
    .compact();

// Validation
Claims claims = parser.parseClaimsJws(token).getBody();
if (!claims.getAudience().equals("https://api.example.com")) {
    throw new InvalidAudienceException();
}
```

### 7. **Issuer (iss) Claim**

**Problem:** Trust tokens only from specific sources.

**Solution:** Add `iss` claim

```java
String token = Jwts.builder()
    .setIssuer("https://auth.example.com") // Who issued this
    .setSubject(userId)
    // ...
    .compact();

// Validation
Claims claims = parser.parseClaimsJws(token).getBody();
if (!claims.getIssuer().equals("https://auth.example.com")) {
    throw new InvalidIssuerException();
}
```

---

## Security Best Practices

### 1. **Algorithm Selection**

| Algorithm | Type | Security | Use Case |
|-----------|------|----------|----------|
| **HS256** | Symmetric | Medium | Single service, shared secret |
| **RS256** | Asymmetric | High | Microservices, public key verification |
| **ES256** | ECDSA | High | Modern, compact, faster |
| **PS256** | RSA-PSS | High | Future-proof |

**Never use:** `none`, `HS512` with weak key

### 2. **Token Expiration**

```
Access Token: 15-30 minutes (short-lived)
Refresh Token: 7-30 days (long-lived)
ID Token: 1 hour (rarely used in SPAs)
```

### 3. **Secure Storage**

#### Frontend
```javascript
// ❌ WRONG: Vulnerable to XSS
localStorage.setItem('token', accessToken);

// ✅ CORRECT: Secure
Set-Cookie: token=jwt; HttpOnly; Secure; SameSite=Strict
```

#### Backend
```java
// ❌ WRONG: Plain text
token stored in database

// ✅ CORRECT: Hashed
String hashedToken = bcrypt.hash(token);
```

### 4. **HTTPS Only**

```
Always use HTTPS for JWT transmission
Token in Authorization header (not URL)
Never log tokens
```

### 5. **CSRF Protection**

```java
http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
```

### 6. **Key Rotation**

```java
// Old key for validation
VerificationKey oldKey = loadKey("keys/old_key.pem", "2024-01");

// New key for signing
SigningKey newKey = loadKey("keys/new_key.pem", "2024-02");

// Support both for transition period
parser.setSigningKey(newKey).verifyWith(oldKey).build();
```

### 7. **JTI (JWT ID) for Tracking**

```java
String token = Jwts.builder()
    .setId(UUID.randomUUID().toString()) // Unique ID
    .setSubject(userId)
    .compact();

// Prevent replay attacks
if (jtiBlacklist.contains(jti)) {
    throw new TokenReplayException();
}
```

---

## Common Interview Questions

### Q1: What are the advantages and disadvantages of JWT?

**Advantages:**
- ✅ Stateless - no server-side session storage
- ✅ Scalable - works across distributed systems
- ✅ Mobile-friendly - can work with any client
- ✅ Self-contained - all info in token

**Disadvantages:**
- ❌ Token revocation is difficult
- ❌ Token size can be large (affects bandwidth)
- ❌ Cannot force logout immediately
- ❌ Secret key compromise affects all tokens

### Q2: What is the difference between Access Token and Refresh Token?

| Aspect | Access Token | Refresh Token |
|--------|--------------|---------------|
| **Lifetime** | Short (15-30 min) | Long (7-30 days) |
| **Purpose** | API access | Get new access token |
| **Storage** | Memory/Cookie | httpOnly Cookie |
| **Revocation** | Hard | Easier (revoke in DB) |
| **Claims** | Minimal | User ID + metadata |

### Q3: How do you prevent JWT token tampering?

- Use strong signing algorithm (RS256, ES256)
- Verify signature on every request
- Use long secret keys (32+ bytes)
- Rotate keys periodically
- Store key securely (KMS, HashiCorp Vault)

### Q4: What is the "none" algorithm vulnerability?

```json
{
  "alg": "none",  // ❌ DANGEROUS
  "typ": "JWT"
}
```

**Risk:** Attacker creates token without signature

**Protection:**
```java
parser.requireSignedClaims()
      .setSigningKey(key)
      .build();
```

### Q5: How does JWT handle CORS?

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://frontend.example.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowCredentials(true)
                    .exposedHeaders("Authorization");
            }
        };
    }
}
```

### Q6: What's the difference between symmetric and asymmetric signing?

**Symmetric (HS256):**
```
Server signs with secret key
Server verifies with same secret key
Risk: If key is compromised, attacker can sign tokens
```

**Asymmetric (RS256):**
```
Server signs with private key
Clients verify with public key
Advantage: Public key can be shared safely
```

### Q7: How do you implement single logout with JWT?

```java
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
    String jwt = extractToken(token);
    String jti = extractJti(jwt);
    long expiresIn = extractExpiresIn(jwt);
    
    // Store JTI in blacklist with TTL
    blacklistService.add(jti, expiresIn);
    
    return ResponseEntity.ok("Logged out");
}

// In filter
if (blacklistService.contains(jti)) {
    throw new TokenRevokedException();
}
```

### Q8: What are common JWT security mistakes?

❌ Storing token in localStorage (XSS vulnerable)
❌ Not validating token signature
❌ Using weak secret keys
❌ Not using HTTPS
❌ Not setting expiration
❌ Storing sensitive data in token
❌ Not rotating refresh tokens
❌ Accepting "none" algorithm

### Q9: How would you implement OAuth 2.0 with JWT?

```
1. User clicks "Login with Google"
2. Redirected to Google auth endpoint
3. User authenticates
4. Google redirects with authorization code
5. Backend exchanges code for access token
6. Backend generates JWT with user info
7. Frontend stores JWT
8. Subsequent requests use JWT
```

### Q10: Explain token lifecycle

```
┌─────────────────────────────────────────────┐
│ 1. User Login                               │
│    - Verify credentials                     │
│    - Generate access + refresh tokens       │
│    - Return to client                       │
└─────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────┐
│ 2. Token Storage (Client)                   │
│    - Access token: Memory/sessionStorage    │
│    - Refresh token: httpOnly Cookie         │
└─────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────┐
│ 3. API Request                              │
│    - Include access token in header         │
│    - Server validates signature + expiry    │
│    - Grant/Deny access                      │
└─────────────────────────────────────────────┘
         ↓ (Access token expires)
┌─────────────────────────────────────────────┐
│ 4. Token Refresh                            │
│    - Send refresh token                     │
│    - Generate new access token              │
│    - Optional: rotate refresh token         │
└─────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────┐
│ 5. Logout                                   │
│    - Blacklist tokens (optional)            │
│    - Clear client-side storage              │
│    - Delete refresh token from DB           │
└─────────────────────────────────────────────┘
```

---

## Code Examples

### Example 1: Secure Token Generation

```java
public String generateSecureToken(String userId, Map<String, Object> claims) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 15 * 60 * 1000); // 15 min

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(userId)
        .setId(UUID.randomUUID().toString()) // JTI for revocation
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .setAudience("https://api.example.com") // Limit token usage
        .setIssuer("https://auth.example.com") // Token source
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

### Example 2: Comprehensive Token Validation

```java
public Claims validateAndParseClaims(String token) {
    try {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .requireIssuer("https://auth.example.com")
            .requireAudience("https://api.example.com")
            .build()
            .parseSignedClaims(token)
            .getPayload();

        // Additional checks
        if (jtiBlacklist.contains(claims.getId())) {
            throw new TokenRevokedException("Token has been revoked");
        }

        return claims;
    } catch (ExpiredJwtException e) {
        throw new TokenExpiredException("Token has expired");
    } catch (JwtException e) {
        throw new InvalidTokenException("Invalid token");
    }
}
```

### Example 3: Role-Based Access

```java
@PostMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminEndpoint() {
    return ResponseEntity.ok("Admin resource");
}

@PostMapping("/moderator")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public ResponseEntity<?> moderatorEndpoint() {
    return ResponseEntity.ok("Moderator resource");
}

@GetMapping("/user-profile")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public ResponseEntity<?> userProfile() {
    return ResponseEntity.ok("User profile");
}
```

### Example 4: Key Rotation

```java
public class KeyRotationService {
    private Map<String, SecretKey> keyStore = new HashMap<>();
    
    public void rotateKeys() {
        String newKeyId = generateKeyId();
        SecretKey newKey = generateNewKey();
        
        keyStore.put(newKeyId, newKey);
        setCurrentKeyId(newKeyId);
    }
    
    public SecretKey getSigningKey() {
        return keyStore.get(getCurrentKeyId());
    }
    
    public SecretKey getKeyById(String keyId) {
        return keyStore.get(keyId);
    }
}
```

---

## Interview Tips

1. **Know the fundamentals:** Header, Payload, Signature
2. **Understand trade-offs:** Stateless vs session-based
3. **Security first:** Can explain why JWT is used correctly
4. **Real-world scenarios:** How would you handle logout, revocation, rotation?
5. **OAuth 2.0:** Know relationship with OAuth 2.0
6. **Alternatives:** Know when NOT to use JWT (Sessions, OAuth, SAML)
7. **Hands-on:** Be ready to code examples

---

## Resources

- JWT.io - Debug and verify JWT
- OAuth 2.0 spec
- OpenID Connect spec
- OWASP JWT Security Guidelines

---

**Good luck with your interview!** 🎯
