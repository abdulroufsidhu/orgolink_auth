# Orgolink Authentication Service

A comprehensive authentication and authorization microservice built with Spring Boot and Kotlin. This service provides JWT-based authentication, project management, and role-based access control with project-specific access tokens.
[System Design](./system_design.md)

## Features

- **User Authentication**: JWT-based authentication with registration and login
- **Project Management**: Create and manage projects with different visibility levels
- **Role-Based Access Control**: Support for OWNER, ADMIN, and USER roles within projects
- **Project Access Tokens**: Generate project-specific tokens for API access
- **Public API**: Public endpoints for project discovery and token validation
- **Multi-tenant Support**: Users can belong to multiple projects with different roles

## Technologies Used

- **Spring Boot 3.5.0** - Application framework
- **Kotlin** - Programming language
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access
- **PostgreSQL** - Database
- **JWT** - Token-based authentication
- **OpenAPI/Swagger** - API documentation
- **Gradle** - Build tool

## Getting Started
### Docker Compose
```bash
curl -O https://raw.githubusercontent.com/abdulroufsidhu/orgolink_auth/refs/heads/master/docker-compose.dev.yml && docker compose -f docker-compose.dev.yml up -d
```

### Prerequisites

- Java 21 or higher
- PostgreSQL database
- Gradle (included via wrapper)

### Database Setup

1. Create a PostgreSQL database
2. Update `application.properties` with your database configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Running the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### API Documentation

Once the application is running, you can access the Swagger UI at:
`http://localhost:8080/public/swagger-ui.html`

## API Endpoints

### Authentication Endpoints

#### Register User
- **POST** `/auth/register`
- **Description**: Register a new user
- **Body**:
```json
{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

#### Login
- **POST** `/auth/login`
- **Description**: Login and receive JWT token
- **Body**:
```json
{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

#### Logout
- **POST** `/auth/logout`
- **Description**: Logout and revoke current token
- **Headers**: `Authorization: Bearer <token>`

#### Verify Token
- **GET** `/auth/verify`
- **Description**: Verify if current token is valid
- **Headers**: `Authorization: Bearer <token>`

### Project Management Endpoints

#### Create Project
- **POST** `/api/projects`
- **Description**: Create a new project (user becomes owner)
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
```json
{
  "name": "My Project",
  "description": "Project description",
  "projectKey": "my-project",
  "isPublic": false
}
```

#### Get User's Projects
- **GET** `/api/projects`
- **Description**: Get all projects where user is a member
- **Headers**: `Authorization: Bearer <token>`

#### Get Project by Key
- **GET** `/api/projects/{projectKey}`
- **Description**: Get specific project details
- **Headers**: `Authorization: Bearer <token>`

#### Update Project
- **PUT** `/api/projects/{projectKey}`
- **Description**: Update project details (requires OWNER or ADMIN role)
- **Headers**: `Authorization: Bearer <token>`

#### Delete Project
- **DELETE** `/api/projects/{projectKey}`
- **Description**: Soft delete project (requires OWNER role)
- **Headers**: `Authorization: Bearer <token>`

### Project User Management

#### Add User to Project
- **POST** `/api/projects/{projectKey}/users`
- **Description**: Add user to project with specified role
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
```json
{
  "username": "user_to_add",
  "role": "USER"
}
```

#### Get Project Users
- **GET** `/api/projects/{projectKey}/users`
- **Description**: Get all users in project
- **Headers**: `Authorization: Bearer <token>`

#### Remove User from Project
- **DELETE** `/api/projects/{projectKey}/users/{username}`
- **Description**: Remove user from project
- **Headers**: `Authorization: Bearer <token>`

### Project Token Management

#### Generate Project Token
- **POST** `/api/projects/{projectKey}/tokens`
- **Description**: Generate project access token
- **Headers**: `Authorization: Bearer <token>`
- **Body**:
```json
{
  "role": "USER",
  "description": "Token for API access",
  "expirationDays": 30
}
```

#### Get Project Tokens
- **GET** `/api/projects/{projectKey}/tokens`
- **Description**: Get all active tokens for project
- **Headers**: `Authorization: Bearer <token>`

#### Revoke Project Token
- **DELETE** `/api/projects/{projectKey}/tokens/{tokenId}`
- **Description**: Revoke specific project token
- **Headers**: `Authorization: Bearer <token>`

#### Get User's Project Tokens
- **GET** `/api/projects/tokens/my`
- **Description**: Get all project tokens created by user
- **Headers**: `Authorization: Bearer <token>`

#### Revoke All User Project Tokens
- **DELETE** `/api/projects/tokens/my`
- **Description**: Revoke all project tokens created by user
- **Headers**: `Authorization: Bearer <token>`

### Public Endpoints

#### Get Public Projects
- **GET** `/public/projects`
- **Description**: Get all public projects (no authentication required)

#### Get Public Project
- **GET** `/public/projects/{projectKey}`
- **Description**: Get public project details (no authentication required)

#### Validate Project Token
- **POST** `/public/projects/{projectKey}/validate-token`
- **Description**: Validate project access token
- **Body**:
```json
{
  "token": "your-project-token"
}
```

#### Health Check
- **GET** `/public/health`
- **Description**: Service health check

## Project Roles

- **OWNER**: Full control over project, can manage users and tokens, can delete project
- **ADMIN**: Can manage users and tokens, cannot delete project
- **USER**: Basic access to project, cannot manage users or tokens

## Authentication Methods

### 1. JWT Authentication (User-based)
- Use JWT tokens obtained from `/auth/login`
- Add header: `Authorization: Bearer <jwt-token>`
- Provides access based on user's project memberships

### 2. Project Access Tokens
- Generate project-specific tokens via `/api/projects/{projectKey}/tokens`
- Use same header format: `Authorization: Bearer <project-token>`
- Provides access to specific project with defined role

## Configuration

Key configuration properties in `application.properties`:

```properties
# JWT Configuration
jwt.secret=your-secret-key
jwt.expiration=86400000  # 24 hours in milliseconds

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password

# Swagger Configuration
springdoc.api-docs.path=/public/api-docs
springdoc.swagger-ui.path=/public/swagger-ui.html
```

## Security Features

- **Password Encryption**: BCrypt with strength 12
- **JWT Security**: Signed tokens with expiration
- **Role-Based Access**: Project-level permissions
- **Token Revocation**: Ability to revoke tokens
- **Input Validation**: Request validation with custom messages
- **CORS Configuration**: Configurable cross-origin requests

## Database Schema

The service uses the following main entities:

- **users**: User accounts
- **projects**: Project definitions
- **project_users**: User-project relationships with roles
- **user_access_tokens**: JWT tokens for user authentication
- **project_access_tokens**: Project-specific access tokens

## Development

### Running Tests
```bash
./gradlew test
```

### Building for Production
```bash
./gradlew bootJar
```

### Docker Support
```dockerfile
FROM openjdk:21-jdk-slim
COPY build/libs/orgolink-auth-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## License

This project is licensed under the MIT License.
