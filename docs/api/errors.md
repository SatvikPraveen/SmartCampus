# Location: docs/api/errors.md

# API Error Handling Guide

## Standard Error Response Format

All API errors follow a consistent format:

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed for field 'email'",
    "path": "/api/users",
    "fieldErrors": {
        "email": "Email format is invalid"
    },
    "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## HTTP Status Codes

### 2xx Success

-   `200 OK` - Request successful
-   `201 Created` - Resource created successfully
-   `204 No Content` - Successful deletion

### 4xx Client Errors

-   `400 Bad Request` - Invalid request data
-   `401 Unauthorized` - Authentication required
-   `403 Forbidden` - Insufficient permissions
-   `404 Not Found` - Resource not found
-   `409 Conflict` - Resource conflict (duplicate email)
-   `422 Unprocessable Entity` - Validation errors
-   `429 Too Many Requests` - Rate limit exceeded

### 5xx Server Errors

-   `500 Internal Server Error` - Unexpected server error
-   `502 Bad Gateway` - External service unavailable
-   `503 Service Unavailable` - System maintenance

## Common Error Scenarios

### Authentication Errors

**Invalid Credentials**

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password",
    "path": "/api/auth/login"
}
```

**Token Expired**

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 401,
    "error": "Token Expired",
    "message": "JWT token has expired",
    "path": "/api/users/profile"
}
```

**Access Denied**

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 403,
    "error": "Access Denied",
    "message": "Insufficient permissions to access this resource",
    "path": "/api/admin/users"
}
```

### Validation Errors

**Field Validation**

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 400,
    "error": "Validation Failed",
    "message": "Input validation failed",
    "path": "/api/users",
    "fieldErrors": {
        "email": "Email format is invalid",
        "firstName": "First name is required",
        "password": "Password must be at least 8 characters"
    }
}
```

### Resource Errors

**Not Found**

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 404,
    "error": "Resource Not Found",
    "message": "Student not found with ID: 12345",
    "path": "/api/students/12345"
}
```

**Conflict**

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 409,
    "error": "Resource Conflict",
    "message": "Email address already exists",
    "path": "/api/users"
}
```

### Rate Limiting

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 429,
    "error": "Too Many Requests",
    "message": "Rate limit exceeded. Try again in 60 seconds",
    "path": "/api/auth/login",
    "retryAfter": 60
}
```

## Error Codes Reference

| Code                       | Description                       | Resolution                                        |
| -------------------------- | --------------------------------- | ------------------------------------------------- |
| `VALIDATION_FAILED`        | Input validation errors           | Fix the validation errors listed in `fieldErrors` |
| `AUTHENTICATION_REQUIRED`  | Missing or invalid authentication | Provide valid JWT token                           |
| `INSUFFICIENT_PERMISSIONS` | User lacks required permissions   | Contact administrator for access                  |
| `RESOURCE_NOT_FOUND`       | Requested resource doesn't exist  | Verify the resource ID and try again              |
| `DUPLICATE_RESOURCE`       | Resource already exists           | Use different unique identifiers                  |
| `RATE_LIMIT_EXCEEDED`      | Too many requests                 | Wait before making more requests                  |
| `EXTERNAL_SERVICE_ERROR`   | External service failure          | Retry later or contact support                    |

## Handling Errors in Client Code

### JavaScript Example

```javascript
async function createStudent(studentData) {
    try {
        const response = await fetch("/api/students", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(studentData),
        });

        if (!response.ok) {
            const error = await response.json();

            switch (response.status) {
                case 400:
                    // Handle validation errors
                    displayValidationErrors(error.fieldErrors);
                    break;
                case 401:
                    // Redirect to login
                    redirectToLogin();
                    break;
                case 409:
                    // Handle conflict (duplicate email)
                    showError("Email already exists");
                    break;
                default:
                    showError("An unexpected error occurred");
            }
            return;
        }

        const student = await response.json();
        return student;
    } catch (error) {
        console.error("Network error:", error);
        showError("Network connection failed");
    }
}
```

### Java Client Example

```java
try {
    ResponseEntity<StudentResponse> response = restTemplate.postForEntity(
        "/api/students", request, StudentResponse.class);
    return response.getBody();

} catch (HttpClientErrorException e) {
    switch (e.getStatusCode()) {
        case BAD_REQUEST:
            // Handle validation errors
            handleValidationErrors(e.getResponseBodyAsString());
            break;
        case UNAUTHORIZED:
            // Handle authentication
            throw new AuthenticationException("Token expired");
        case CONFLICT:
            // Handle conflicts
            throw new BusinessException("Student already exists");
        default:
            throw new ApiException("API call failed", e);
    }
}
```

## Troubleshooting Common Issues

### Issue: Getting 401 Unauthorized

1. Check if JWT token is included in Authorization header
2. Verify token hasn't expired
3. Ensure token format: `Bearer {token}`

### Issue: Getting 403 Forbidden

1. Check user role permissions
2. Verify user is accessing allowed resources
3. Contact administrator if permissions seem correct

### Issue: Getting 400 Bad Request

1. Review the `fieldErrors` object in response
2. Fix validation issues mentioned
3. Ensure request format matches API specification

### Issue: Getting 500 Internal Server Error

1. Check server logs for detailed error information
2. Verify all required services are running
3. Contact support team with correlation ID

## Support

For persistent API errors:

1. Check the correlation ID in error responses
2. Review server logs with the correlation ID
3. Contact support with the correlation ID and request details
4. Include steps to reproduce the error
