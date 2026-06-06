# JWT Token Service - cURL Commands Guide

This document provides all cURL commands for testing the JWT Token Service API.

## Base URL
```
http://localhost:8085
```

## Prerequisites
- cURL installed
- Application running on port 8085
- (Optional) `jq` for JSON formatting

---

## 1. LOGIN - Get JWT Token

Generate a JWT token using username and password.

### Command:
```bash
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "password"
  }'
```

### Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VySWQiOjEsInN1YiI6InVzZXIiLCJpYXQiOjE3MjA1NTMwMjMsImV4cCI6MTcyMDYzOTQyM30.aBc123...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "user",
  "email": "user@example.com"
}
```

### Test Credentials:
- **User1**: `username: user` | `password: password`
- **User2**: `username: admin` | `password: admin123`

---

## 2. VALIDATE TOKEN

Check if a JWT token is valid and not expired.

### Command:
```bash
curl -X POST http://localhost:8085/api/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

### Response (Valid):
```json
{
  "valid": true,
  "message": "Token is valid",
  "username": "user"
}
```

### Response (Invalid):
```json
{
  "valid": false,
  "message": "Token is invalid or expired"
}
```

---

## 3. GET CURRENT USER (Protected)

Retrieve current authenticated user information. Requires valid JWT token.

### Command:
```bash
curl -X GET http://localhost:8085/api/auth/user \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

### Response:
```json
{
  "id": 1,
  "username": "user",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Error (No Token):
```json
{
  "error": "Unauthorized",
  "message": "No authenticated user found"
}
```

---

## 4. REFRESH TOKEN

Generate a new JWT token from an existing one.

### Command:
```bash
curl -X POST http://localhost:8085/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

### Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VySWQiOjEsInN1YiI6InVzZXIiLCJpYXQiOjE3MjA1NTAyMzAsImV4cCI6MTcyMDYzNjYzMH0.xyz789...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "user",
  "email": "user@example.com"
}
```

---

## Complete Workflow Example

### Step 1: Login and capture token
```bash
TOKEN=$(curl -s -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "password"
  }' | jq -r '.accessToken')

echo "Token: $TOKEN"
```

### Step 2: Use token to get user info
```bash
curl -X GET http://localhost:8085/api/auth/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq .
```

### Step 3: Validate the token
```bash
curl -X POST http://localhost:8085/api/auth/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\"}" | jq .
```

### Step 4: Refresh the token
```bash
NEW_TOKEN=$(curl -s -X POST http://localhost:8085/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$TOKEN\"}" | jq -r '.accessToken')

echo "New Token: $NEW_TOKEN"
```

---

## Error Responses

### 401 Unauthorized
```json
{
  "error": "Authentication failed",
  "message": "Bad credentials"
}
```

### 401 Invalid Token
```json
{
  "error": "Invalid token",
  "message": "The provided token is invalid or expired"
}
```

### 500 Server Error
```json
{
  "error": "Error",
  "message": "Error description"
}
```

---

## Common Issues & Solutions

### Issue: "Connection refused"
- **Solution**: Ensure application is running on port 8085
  ```bash
  curl http://localhost:8085/api/auth/user
  # Should return 401 (not connected error)
  ```

### Issue: "Invalid token"
- **Solution**: Ensure token format is correct and not expired
  - Token should start with `eyJ...`
  - Token expires after 24 hours

### Issue: cURL command not found
- **Solution**: 
  - **Windows**: Download from https://curl.se/download.html
  - **Linux/Mac**: Install via package manager
    ```bash
    # Linux
    sudo apt-get install curl
    
    # Mac
    brew install curl
    ```

### Issue: JSON formatting in cURL
- **Solution**: Install jq for pretty printing
  ```bash
  # Linux
  sudo apt-get install jq
  
  # Mac
  brew install jq
  
  # Windows (with Chocolatey)
  choco install jq
  ```

---

## Quick Reference

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/api/auth/login` | POST | No | Get JWT token |
| `/api/auth/validate` | POST | No | Validate token |
| `/api/auth/refresh` | POST | No | Refresh token |
| `/api/auth/user` | GET | Yes | Get user info |

---

## Script Files

- **Linux/Mac**: Use `curl_commands.sh`
  ```bash
  bash curl_commands.sh
  ```

- **Windows**: Use `curl_commands.bat`
  ```cmd
  curl_commands.bat
  ```

---

## Additional Resources

- [cURL Manual](https://curl.se/docs/manpage.html)
- [JWT Debugger](https://jwt.io)
- [HTTP Status Codes](https://httpwg.org/specs/rfc7231.html#status.codes)

---

**Last Updated**: June 2024
**API Version**: 1.0.0
