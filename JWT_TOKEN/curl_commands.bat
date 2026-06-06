@echo off
REM JWT Token Service - cURL Commands for Windows
REM Base URL
set BASE_URL=http://localhost:8085

REM ============================================
REM 1. LOGIN - Get JWT Token
REM ============================================
echo === 1. LOGIN ===
curl -X POST %BASE_URL%/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"user\",\"password\":\"password\"}"

echo.
echo === 2. VALIDATE TOKEN ===
REM Copy the token from login response and paste it here
set TOKEN=YOUR_TOKEN_HERE
curl -X POST %BASE_URL%/api/auth/validate ^
  -H "Content-Type: application/json" ^
  -d "{\"token\":\"%TOKEN%\"}"

echo.
echo === 3. GET CURRENT USER (Protected) ===
curl -X GET %BASE_URL%/api/auth/user ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json"

echo.
echo === 4. REFRESH TOKEN ===
curl -X POST %BASE_URL%/api/auth/refresh ^
  -H "Content-Type: application/json" ^
  -d "{\"token\":\"%TOKEN%\"}"

pause
