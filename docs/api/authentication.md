# Location: docs/api/authentication.md

# Authentication API Guide

## Overview

SmartCampus uses JWT (JSON Web Token) based authentication with role-based access control (RBAC). This guide covers authentication flows, token management, and security best practices.

## Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Database

    Client->>API: POST /api/auth/login
    API->>Database: Validate credentials
    Database-->>API: User data
    API-->>Client: JWT token + user info

    Client->>API: API request with Bearer token
    API->>API: Validate JWT
    API-->>Client: Protected resource
```

## Endpoints

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@smartcampus.com",
  "password": "securePassword123"
}
```

**Response:**

```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "user": {
        "id": 1,
        "email": "user@smartcampus.com",
        "role": "STUDENT",
        "firstName": "John",
        "lastName": "Doe"
    }
}
```

### Token Refresh

```http
POST /api/auth/refresh
Authorization: Bearer {refresh_token}
```

### Logout

```http
POST /api/auth/logout
Authorization: Bearer {access_token}
```

### Password Reset

```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@smartcampus.com"
}
```

## User Roles

| Role      | Description           | Permissions                       |
| --------- | --------------------- | --------------------------------- |
| `STUDENT` | Student users         | View courses, enroll, view grades |
| `FACULTY` | Faculty members       | Manage courses, submit grades     |
| `ADMIN`   | System administrators | Full system access                |
| `STAFF`   | Support staff         | Limited administrative access     |

## Using JWT Tokens

Include the JWT token in the Authorization header:

```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Token Structure

```json
{
    "header": {
        "alg": "HS256",
        "typ": "JWT"
    },
    "payload": {
        "sub": "user@smartcampus.com",
        "iat": 1640995200,
        "exp": 1641081600,
        "roles": ["STUDENT"],
        "userId": 12345
    }
}
```

## Error Responses

### Invalid Credentials

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password",
    "path": "/api/auth/login"
}
```

### Token Expired

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 401,
    "error": "Token Expired",
    "message": "JWT token has expired",
    "path": "/api/users/profile"
}
```

## Security Best Practices

1. **Store tokens securely** - Use secure HTTP-only cookies or secure storage
2. **Implement token refresh** - Use short-lived access tokens with refresh tokens
3. **Validate on server** - Always validate tokens server-side
4. **Use HTTPS** - Never send tokens over unencrypted connections
5. **Handle expiration** - Implement proper token refresh logic
