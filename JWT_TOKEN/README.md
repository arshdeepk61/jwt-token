# JWT Token Service - Spring Security with JWT Authentication

A comprehensive Spring Boot application implementing JWT (JSON Web Token) authentication with Spring Security. This project provides a complete authentication system with login, token refresh, and token validation endpoints.

## Features

✅ **JWT Token Generation** - Secure token creation with HS256 algorithm
✅ **Token Validation** - Validate JWT tokens and extract claims
✅ **Token Refresh** - Refresh expired tokens to maintain sessions
✅ **Spring Security Integration** - Stateless JWT-based authentication
✅ **H2 Database** - In-memory database for user storage
✅ **BCrypt Password Encoding** - Secure password hashing
✅ **CORS Support** - Cross-origin resource sharing enabled
✅ **Postman Collection** - Ready-to-use API testing collection

## Architecture

```
JWT Token Service
├── Config (SecurityConfig)
├── Controller (AuthController)
├── Security (JwtTokenProvider, JwtAuthenticationFilter)
├── Model (User Entity)
├── Repository (UserRepository)
└── DTOs (Request/Response Objects)
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Postman (for API testing)

## Setup & Building

### Clone the Repository
```bash
git clone <repository-url>
cd JWT_TOKEN
```

### Build the Project
```bash
mvn clean install
```

### Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8085`

## Configuration

Edit `src/main/resources/application.properties`:

```properties
server.port=8085

# JWT Settings
jwt.secret=mySecretKeyForJWTTokenGenerationAndValidationPurposesOnly1234567890
jwt.expiration=86400000  # 24 hours in milliseconds

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## API Endpoints

### 1. **Login**
Generate JWT token using credentials.

```http
POST http://localhost:8085/api/auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "user",
  "email": "user@example.com"
}
```

### 2. **Refresh Token**
Refresh an existing JWT token.

```http
POST http://localhost:8085/api/auth/refresh
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "user",
  "email": "user@example.com"
}
```

### 3. **Validate Token**
Check if a JWT token is valid.

```http
POST http://localhost:8085/api/auth/validate
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response:**
```json
{
  "valid": true,
  "message": "Token is valid",
  "username": "user"
}
```

### 4. **Get Current User** (Protected)
Get authenticated user information.

```http
GET http://localhost:8085/api/auth/user
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response:**
```json
{
  "id": 1,
  "username": "user",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

## Default Users

The application includes two pre-configured users:

| Username | Password | Role |
|----------|----------|------|
| user | password | USER |
| admin | admin123 | ADMIN |

## Testing with Postman

1. Import `jwt_token_collection.json` into Postman
2. Execute **Login** endpoint to get a token
3. Copy the token and paste in other requests' Authorization header
4. Test all endpoints

### Manual Testing Steps:
```bash
# 1. Login and get token
curl -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'

# 2. Use token in subsequent requests
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8085/api/auth/user
```

## Database Access

Access H2 Console at: `http://localhost:8085/h2-console`

- **JDBC URL:** `jdbc:h2:mem:testdb`
- **User:** `sa`
- **Password:** (leave empty)

## Project Structure

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── JwtTokenApplication.java       # Main Spring Boot app
│   │   ├── config/
│   │   │   └── SecurityConfig.java        # Security configuration
│   │   ├── controller/
│   │   │   └── AuthController.java        # REST endpoints
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java      # JWT generation/validation
│   │   │   └── JwtAuthenticationFilter.java # JWT filter
│   │   ├── model/
│   │   │   └── User.java                  # User entity
│   │   ├── repository/
│   │   │   └── UserRepository.java        # User data access
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── TokenResponse.java
│   │       ├── RefreshTokenRequest.java
│   │       ├── ValidateTokenRequest.java
│   │       ├── ValidateTokenResponse.java
│   │       └── UserResponse.java
│   └── resources/
│       └── application.properties         # Configuration
├── test/
│   └── java/com/example/
└── pom.xml                               # Maven configuration
```

## Key Technologies

- **Spring Boot 3.1.5** - Application framework
- **Spring Security 6.x** - Authentication & authorization
- **JJWT 0.12.3** - JWT library
- **Spring Data JPA** - ORM framework
- **H2 Database** - In-memory database
- **Lombok** - Boilerplate code reduction
- **Jakarta Servlet** - Servlet API

## Security Features

1. **Stateless Authentication** - Uses JWT tokens instead of sessions
2. **BCrypt Hashing** - Passwords are securely hashed
3. **CORS Configuration** - Configurable cross-origin requests
4. **Secure Secret Key** - HMAC-SHA256 signing algorithm
5. **Token Expiration** - Configurable token lifetime
6. **JWT Filter** - Automatic token extraction and validation

## Error Handling

All endpoints return appropriate HTTP status codes:

- **200 OK** - Successful request
- **401 Unauthorized** - Invalid credentials or expired token
- **500 Internal Server Error** - Server-side errors

## Common Issues

### Issue: mvn command not found
**Solution:** Ensure Maven is installed and added to system PATH
```bash
# Install Maven (Windows - using choco)
choco install maven

# Or download from https://maven.apache.org/download.cgi
```

### Issue: Port 8085 already in use
**Solution:** Change port in `application.properties`
```properties
server.port=8086
```

### Issue: H2 console not accessible
**Solution:** Ensure H2 is enabled and headers are configured correctly in SecurityConfig

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

MIT License - See LICENSE file for details

## Support

For issues or questions, please open an issue in the repository.

---

**Last Updated:** June 2024
**Version:** 1.0.0
