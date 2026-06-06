# JWT Token Interview Questions & Answers

Comprehensive guide for JWT interview preparation with real-world scenarios.

---

## Table of Contents
1. [Beginner Level Questions](#beginner-level)
2. [Intermediate Level Questions](#intermediate-level)
3. [Advanced Level Questions](#advanced-level)
4. [System Design Questions](#system-design)
5. [Coding Questions](#coding-questions)

---

## Beginner Level

### Q1: What is JWT and why is it used?

**Answer:**
JWT (JSON Web Token) is a stateless, self-contained token used for authentication and information exchange between parties.

**Why JWT?**
- **Stateless**: No need for server-side session storage
- **Scalable**: Works across distributed systems and microservices
- **Mobile-friendly**: Can be used with any client
- **Self-contained**: All information is in the token

**Example Flow:**
```
Client Login → Server validates → Issues JWT → Client stores JWT
Client API call → Includes JWT in header → Server validates JWT → Grants access
```

---

### Q2: Explain the structure of JWT

**Answer:**
JWT has three parts separated by dots: `Header.Payload.Signature`

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload (Claims):**
```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022,
  "exp": 1516325422
}
```

**Signature:**
```
HMACSHA256(base64(header) + "." + base64(payload), secret)
```

---

### Q3: What are JWT claims?

**Answer:**
Claims are statements about an entity (user). Three types:

1. **Registered Claims** (Standard):
   - `iss` (issuer)
   - `sub` (subject)
   - `aud` (audience)
   - `exp` (expiration)
   - `nbf` (not before)
   - `iat` (issued at)
   - `jti` (JWT ID)

2. **Public Claims**: Can be defined at will
3. **Private Claims**: Application-specific claims

**Example:**
```json
{
  "iss": "https://auth.example.com",
  "sub": "user123",
  "exp": 1516325422,
  "roles": ["USER", "ADMIN"],
  "email": "user@example.com"
}
```

---

### Q4: What is the difference between authentication and authorization?

**Answer:**

| Aspect | Authentication | Authorization |
|--------|----------------|---------------|
| **Definition** | Verifying user identity | Verifying user permissions |
| **Question** | "Who are you?" | "What can you do?" |
| **Method** | Username/password, JWT | Roles, permissions |
| **Example** | Login process | Access control |

**Flow:**
```
Authentication → User provides credentials → JWT issued
Authorization → Check JWT claims → Grant/Deny access
```

---

### Q5: Why is HTTPS important for JWT?

**Answer:**
- **Prevents interception**: HTTPS encrypts token in transit
- **Prevents man-in-the-middle attacks**: No one can capture the token
- **Token confidentiality**: Encrypted communication channel

**Bad:**
```
http://api.example.com/user (Token exposed)
```

**Good:**
```
https://api.example.com/user (Token encrypted)
```

---

## Intermediate Level

### Q6: How do you handle JWT token expiration?

**Answer:**
Use the `exp` claim with a timestamp.

```java
// Token expires in 15 minutes
long expirationTime = System.currentTimeMillis() + 15 * 60 * 1000;
Date expiryDate = new Date(expirationTime);

String token = Jwts.builder()
    .setExpiration(expiryDate)
    .signWith(key)
    .compact();

// Validation
try {
    parser.parseClaimsJws(token);
} catch (ExpiredJwtException e) {
    // Token has expired - ask user to login again
}
```

---

### Q7: What's the difference between symmetric and asymmetric signing?

**Answer:**

**Symmetric (HS256):**
```
Both parties share same secret key
Sign: HMAC(secret, payload)
Verify: HMAC(secret, payload) == signature

Advantage: Simple, fast
Disadvantage: If secret is compromised, anyone can forge tokens
```

**Asymmetric (RS256):**
```
Server has private key, clients have public key
Sign: RSA(private_key, payload)
Verify: RSA(public_key, payload) == signature

Advantage: Public key can be shared safely
Disadvantage: Slower, more complex
```

**When to use:**
- **HS256**: Single service, internal use
- **RS256**: Microservices, external APIs, OAuth 2.0

---

### Q8: How do you refresh a JWT token?

**Answer:**
Use a refresh token strategy:

```java
// Initial login
String accessToken = generateAccessToken(user, 15 * 60 * 1000); // 15 min
String refreshToken = generateRefreshToken(user, 7 * 24 * 60 * 60 * 1000); // 7 days

// When access token expires
POST /refresh
Body: { "refreshToken": "..." }

// Server validates refresh token
if (isValidRefreshToken(refreshToken)) {
    String newAccessToken = generateAccessToken(user);
    return { "accessToken": newAccessToken };
} else {
    throw UnauthorizedException("Refresh token expired, login required");
}
```

---

### Q9: What is token blacklisting and why is it needed?

**Answer:**
Token blacklisting is maintaining a list of revoked tokens.

**Problem:** Once JWT is issued, it's valid until expiration. Can't revoke early (logout).

**Solution:** Blacklist approach

```java
// Logout
POST /logout
Header: Authorization: Bearer <token>

// Server adds token to blacklist
blacklist.add(jti);

// On validation
if (blacklist.contains(jti)) {
    throw TokenRevokedException();
}
```

**Blacklist Storage:**
- **Redis**: Recommended (fast, TTL-based cleanup)
- **Database**: Persistent
- **In-memory**: Fast but loses on restart

---

### Q10: Explain RBAC and PBAC with JWT

**Answer:**

**Role-Based Access Control (RBAC):**
```json
{
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```

```java
@PostMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminEndpoint() {
    return ResponseEntity.ok("Admin only");
}
```

**Permission-Based Access Control (PBAC):**
```json
{
  "permissions": ["read:user", "write:user", "delete:user"]
}
```

```java
@PostMapping("/delete-user")
@PreAuthorize("hasAuthority('delete:user')")
public ResponseEntity<?> deleteUser() {
    return ResponseEntity.ok("User deleted");
}
```

**When to use:**
- **RBAC**: Simple permission model (few roles)
- **PBAC**: Complex permission model (many operations)

---

## Advanced Level

### Q11: How do you prevent JWT attacks?

**Answer:**

**1. None Algorithm Attack:**
```json
{
  "alg": "none"  // Attacker can forge token
}
```

**Prevention:**
```java
parser.requireSignedClaims().build();
```

**2. Token Tampering:**
```
Attacker modifies payload but signature doesn't match
```

**Prevention:**
```java
// Always verify signature
parser.verifyWith(signingKey).build();
```

**3. Man-in-the-Middle:**
```
Token captured over HTTP
```

**Prevention:**
- Always use HTTPS
- Set HttpOnly cookie flag
- Use Secure cookie flag

**4. Replay Attack:**
```
Attacker reuses captured token
```

**Prevention:**
- Add JTI (JWT ID)
- Track used tokens
- Short expiration time

---

### Q12: What is audience (aud) and issuer (iss) in JWT?

**Answer:**

**Audience (aud):**
- Specifies who the token is intended for
- Prevents token from being used on wrong service

```java
// Issue token for API service
String token = Jwts.builder()
    .setAudience("https://api.example.com")
    .compact();

// Validation
Claims claims = parser.parseClaimsJws(token).getBody();
if (!claims.getAudience().equals("https://api.example.com")) {
    throw new InvalidAudienceException();
}
```

**Issuer (iss):**
- Specifies who created the token
- Ensures trust - only trust tokens from authorized issuers

```java
String token = Jwts.builder()
    .setIssuer("https://auth.example.com")
    .compact();

// Validation
if (!claims.getIssuer().equals("https://auth.example.com")) {
    throw new InvalidIssuerException();
}
```

**Real-world example:**
```
Microservice architecture:
- Auth service issues token with iss: auth.example.com
- API service only accepts aud: api.example.com
- Payment service only accepts aud: payment.example.com
```

---

### Q13: Explain key rotation in JWT

**Answer:**
Periodically changing signing keys to mitigate key compromise risks.

```java
// Store multiple keys
Map<String, SecretKey> keyStore = {
    "2024-01": oldKey,
    "2024-02": currentKey,
    "2024-03": newKey
}

// Sign with current key
String token = Jwts.builder()
    .signWith(keyStore.get("2024-02"))
    .compact();

// Validate with multiple keys (for transition period)
for (SecretKey key : keyStore.values()) {
    try {
        parser.verifyWith(key).build().parseClaimsJws(token);
        return true;
    } catch (SignatureException e) {
        // Try next key
    }
}
```

---

### Q14: How do you implement logout with JWT?

**Answer:**
Use blacklisting or JTI revocation:

```java
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader("Authorization") String auth) {
    String token = auth.substring(7); // Remove "Bearer "
    String jti = extractJti(token);
    long expiresIn = getExpiresIn(token);
    
    // Add to blacklist
    blacklistService.addToBlacklist(jti, expiresIn);
    
    // Clear session
    SecurityContextHolder.clearContext();
    
    return ResponseEntity.ok("Logged out");
}

// On future requests
if (blacklistService.isBlacklisted(jti)) {
    throw new TokenRevokedException();
}
```

---

### Q15: What is token rotation?

**Answer:**
Periodically issuing new tokens and revoking old ones.

```java
public TokenResponse rotateTokens(String currentRefreshToken) {
    // Validate
    if (!isValidRefreshToken(currentRefreshToken)) {
        throw new InvalidTokenException();
    }
    
    // Extract user
    String userId = extractUserId(currentRefreshToken);
    
    // Generate new tokens
    String newAccessToken = generateAccessToken(userId);
    String newRefreshToken = generateRefreshToken(userId);
    
    // Revoke old token
    revokeRefreshToken(currentRefreshToken);
    
    // Save new token
    saveRefreshToken(userId, newRefreshToken);
    
    return new TokenResponse(newAccessToken, newRefreshToken);
}
```

**Benefits:**
- Limits damage from compromised token
- Forces re-authentication periodically
- Maintains security posture

---

## System Design

### Q16: Design an authentication system using JWT

**Answer:**

```
┌──────────────┐
│   Frontend   │
└──────────────┘
      │
      ├─→ POST /login
      │   (username, password)
      │
      ├─← 200 OK
      │   {
      │     "accessToken": "...",
      │     "refreshToken": "...",
      │     "expiresIn": 900000
      │   }
      │
      ├─→ GET /api/user
      │   Header: Authorization: Bearer <accessToken>
      │
      ├─← 200 OK (if token valid)
      │
      ├─← 401 Unauthorized (if token expired)
      │
      ├─→ POST /refresh
      │   Body: { "refreshToken": "..." }
      │
      └─← New accessToken


┌──────────────┐
│   Backend    │
└──────────────┘
  │
  ├─ AuthController
  │  ├─ POST /login
  │  ├─ POST /refresh
  │  └─ POST /logout
  │
  ├─ JwtTokenProvider
  │  ├─ generateToken()
  │  ├─ validateToken()
  │  └─ extractClaims()
  │
  ├─ JwtAuthenticationFilter
  │  ├─ Extract token from header
  │  ├─ Validate token
  │  └─ Set SecurityContext
  │
  ├─ TokenBlacklistService
  │  ├─ addToBlacklist()
  │  └─ isBlacklisted()
  │
  └─ UserRepository
     └─ findByUsername()
```

---

## Coding Questions

### Q17: Implement basic JWT token generation

```java
public String generateToken(String userId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 15 * 60 * 1000);
    
    return Jwts.builder()
        .setSubject(userId)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

### Q18: Implement JWT token validation

```java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}
```

### Q19: Implement token refresh logic

```java
public String refreshToken(String token) {
    if (!validateToken(token)) {
        throw new InvalidTokenException();
    }
    
    String userId = extractUserId(token);
    return generateToken(userId);
}
```

### Q20: Implement logout with blacklist

```java
public void logout(String token) {
    String jti = extractJti(token);
    long expiresIn = getExpiresIn(token);
    blacklist.put(jti, System.currentTimeMillis() + expiresIn);
}

public boolean isLoggedOut(String token) {
    String jti = extractJti(token);
    return blacklist.containsKey(jti);
}
```

---

## Key Takeaways

✅ JWT is stateless and scalable
✅ Always use HTTPS
✅ Implement token expiration
✅ Use refresh tokens for better security
✅ Blacklist tokens for logout
✅ Validate signature every time
✅ Use RS256 for microservices
✅ Implement key rotation
✅ Add audience and issuer claims
✅ Never accept "none" algorithm

---

## Resources for More Learning

- https://jwt.io - JWT debugger
- https://tools.ietf.org/html/rfc7519 - JWT specification
- https://auth0.com/blog/jwt-authentication-best-practices/ - Best practices
- https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html - OWASP JWT guide

---

**Good luck with your interview!** 🚀
