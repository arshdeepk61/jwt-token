#!/bin/bash

# JWT Token Service - cURL Commands
# Base URL
BASE_URL="http://localhost:8085"

# ============================================
# 1. LOGIN - Get JWT Token
# ============================================
echo "=== 1. LOGIN ==="
curl -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "password"
  }' | jq .

# Store token in variable (requires jq installed)
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "password"
  }' | jq -r '.accessToken')

echo "Token: $TOKEN"

# ============================================
# 2. VALIDATE TOKEN
# ============================================
echo -e "\n=== 2. VALIDATE TOKEN ==="
curl -X POST $BASE_URL/api/auth/validate \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$TOKEN\"
  }" | jq .

# ============================================
# 3. GET CURRENT USER (Protected)
# ============================================
echo -e "\n=== 3. GET CURRENT USER ==="
curl -X GET $BASE_URL/api/auth/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq .

# ============================================
# 4. REFRESH TOKEN
# ============================================
echo -e "\n=== 4. REFRESH TOKEN ==="
curl -X POST $BASE_URL/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$TOKEN\"
  }" | jq .
