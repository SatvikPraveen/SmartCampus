# Location: docs/api/README.md

# SmartCampus API Documentation

This directory contains comprehensive API documentation for the SmartCampus Backend system.

## Quick Links

-   **Interactive API Documentation**: [Swagger UI](http://localhost:8080/swagger-ui.html)
-   **OpenAPI Specification**: [JSON Format](http://localhost:8080/v3/api-docs)
-   **Postman Collection**: [Download](./postman/SmartCampus-API.postman_collection.json)

## Documentation Structure

-   `authentication.md` - Authentication and authorization guide
-   `endpoints/` - Detailed endpoint documentation by module
-   `examples/` - Request/response examples and use cases
-   `postman/` - Postman collections and environment files
-   `schemas/` - Data models and schema definitions
-   `errors.md` - Error codes and troubleshooting guide

## API Modules

### Core Modules

-   **Authentication API** (`/api/auth/*`) - User authentication and JWT management
-   **User Management API** (`/api/users/*`) - User CRUD operations and profiles
-   **Academic Management API** (`/api/courses/*`, `/api/enrollments/*`) - Course and enrollment management
-   **Facility Management API** (`/api/facilities/*`, `/api/bookings/*`) - Facility booking system

### System APIs

-   **Health Check API** (`/actuator/health`) - System health monitoring
-   **Metrics API** (`/actuator/metrics`) - Application metrics
-   **Admin API** (`/api/admin/*`) - Administrative operations

## Getting Started

1. **Start the application**:

    ```bash
    docker-compose up -d
    ```

2. **Access interactive documentation**:

    ```
    http://localhost:8080/swagger-ui.html
    ```

3. **Authenticate** (for protected endpoints):
    ```bash
    curl -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"email":"admin@smartcampus.com","password":"admin123"}'
    ```

## API Versioning

The API currently supports:

-   **Version 1.0**: Base API functionality
-   **Future versions**: Will maintain backward compatibility

## Rate Limiting

All API endpoints are subject to rate limiting:

-   **Default limit**: 100 requests per minute per IP
-   **Authenticated users**: 1000 requests per minute
-   **Admin users**: 5000 requests per minute

## Support

For API-related questions:

-   Check the interactive documentation first
-   Review the examples in this directory
-   Open an issue on GitHub
-   Contact the development team at dev-team@smartcampus.com
