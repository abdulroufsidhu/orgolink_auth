# OrgoLink Auth Microservice

A Spring Boot microservice providing stateless JWT authentication with role-based access control (RBAC) and permission management.

## Features

- **Stateless JWT Authentication**: Secure token-based authentication
- **Role-Based Access Control**: Flexible role and permission system
- **Redis Caching**: High-performance token caching
- **PostgreSQL Database**: Reliable data persistence with WAL enabled for Debezium
- **Audit Trail**: Track user creation and modifications
- **Soft/Hard Delete**: Configurable user deletion strategy
- **Password Reset**: Token-based password reset flow
- **Modern UI**: Beautiful Thymeleaf templates for login, register, and password reset
- **Health Check**: Docker-compatible health endpoint

## Tech Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3.2.1
- **Database**: PostgreSQL 16
- **Caching**: Redis 7
- **Migration**: Flyway
- **Security**: Spring Security + JWT
- **Templates**: Thymeleaf

## Prerequisites

- Java 17 or higher
- Docker & Docker Compose (for PostgreSQL and Redis)
- Gradle 8.5+ (included via wrapper)

## Quick Start

### 1. Start PostgreSQL and Redis

```bash
docker-compose up -d
```

### 2. Build the Application

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Configuration

Configuration is managed through `application.yml`. Environment variables can be provided via Docker Compose or system environment.

### Key Configuration Properties

```yaml
orgolink:
  auth:
    jwt:
      secret: ${ORGOLINK_AUTH_JWT_SECRET}  # JWT signing secret (min 256 bits)
      expiration-ms: 86400000              # Token expiration (24 hours)
    delete-scheme: soft                     # User deletion: "soft" or "hard"
    pass-reset-timeout: 3600000             # Password reset token timeout (1 hour)
    roles:                                  # Define roles and their permissions
      - name: ADMIN
        permissions: [USER_READ, USER_WRITE, USER_DELETE, ...]
      - name: USER
        permissions: [USER_READ, PROFILE_UPDATE]
```

See `application-example.yml` for a complete documented configuration template.

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/register` | Register new user | No |
| POST | `/api/login` | User login | No |
| POST | `/api/forgot-password` | Request password reset | No |
| POST | `/api/reset-password/{token}` | Reset password | No |

### Verification

| Method | Endpoint | Description | Auth Required | Returns |
|--------|----------|-------------|---------------|---------|
| GET | `/api/verify` | Verify JWT token | Yes | 200 (valid) / 401 (invalid) |
| GET | `/api/verify_role?role=ADMIN` | Verify user role | Yes | 200 (authorized) / 403 (forbidden) |
| GET | `/api/verify_permission?permission=USER_DELETE` | Verify permission | Yes | 200 (authorized) / 403 (forbidden) |

### User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/user` | Get authenticated user | Yes |
| DELETE | `/api/delete` | Delete own account | Yes |
| DELETE | `/api/delete/{uid}` | Delete specific user | Yes (Admin) |

### Health Check

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/healthy` | Health check for monitoring | No |

## UI Pages

| URL | Description |
|-----|-------------|
| `/login` | Login page |
| `/register` | Registration page |
| `/forgot-password` | Forgot password page |
| `/new-password/{token}` | Password reset page |

## Authentication Flow

### Registration

```bash
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securepass123"
  }'
```

Response:
```json
{
  "status": "success",
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "roles": ["USER"],
      "createdAt": "2026-01-08T19:22:59"
    }
  }
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "securepass123"
  }'
```

### Using JWT Token

```bash
curl -X GET http://localhost:8080/api/user \
  -H "Authorization: Bearer {your-jwt-token}"
```

## Role and Permission System

Roles and permissions are configured in `application.yml`:

```yaml
orgolink:
  auth:
    roles:
      - name: ADMIN
        permissions:
          - USER_READ
          - USER_WRITE
          - USER_DELETE
          - USER_CREATE
          - ROLE_MANAGE
          - SYSTEM_CONFIG
      
      - name: USER
        permissions:
          - USER_READ
          - PROFILE_UPDATE
```

Permissions are automatically included in JWT tokens and can be verified via the API.

## Database Schema

The database includes the following tables:

- **users**: User accounts with audit fields
- **roles**: User roles
- **permissions**: System permissions
- **user_roles**: User-role relationships
- **role_permissions**: Role-permission relationships (backup, primarily config-based)
- **tokens**: JWT token storage
- **password_reset_tokens**: Password reset tokens

### Write-Ahead Logging (WAL)

PostgreSQL WAL is enabled in the first migration (`V1__enable_wal.sql`) to support future Debezium integration for change data capture.

## Redis Caching

Tokens are cached in Redis with TTL matching the JWT expiration time. This provides:

- Fast token verification (no database hit)
- Automatic token expiration
- Easy token invalidation

Redis keys follow the pattern: `auth:token:{token_value}`

## Soft vs Hard Delete

The deletion scheme is configurable via `orgolink.auth.delete-scheme`:

- **soft** (default): Marks users as deleted, retains data
- **hard**: Permanently removes user data

## Development

### Running Tests

```bash
./gradlew test
```

### Building for Production

```bash
./gradlew clean build
```

The JAR file will be available at `build/libs/orgolink-auth-0.0.1-SNAPSHOT.jar`

## Docker Support

To run the entire application in Docker, uncomment the `app` service in `docker-compose.yml` and create a `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/orgolink-auth-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Security Considerations

### Production Deployment

1. **JWT Secret**: Use a strong, randomly generated secret (min 256 bits)
2. **Environment Variables**: Never commit secrets to version control
3. **HTTPS**: Always use HTTPS in production
4. **CORS**: Configure CORS properly for your frontend domains
5. **Password Policy**: Implement strong password requirements
6. **Rate Limiting**: Add rate limiting to prevent brute force attacks

### Environment Variables for Production

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/orgolink_auth
export SPRING_DATASOURCE_USERNAME=your_db_user
export SPRING_DATASOURCE_PASSWORD=your_db_password
export SPRING_REDIS_HOST=your-redis-host
export SPRING_REDIS_PORT=6379
export ORGOLINK_AUTH_JWT_SECRET=your-super-secret-jwt-key-min-256-bits
export ORGOLINK_AUTH_DELETE_SCHEME=soft
```

## License

This project is proprietary and confidential.

## Support

For issues or questions, please contact the development team.
