# API

The primary application is server-rendered. A small versioned JSON API supports catalog integrations and provides an extension point for future clients.

## Authentication

API requests use the same authenticated session as the web application. Anonymous API access is disabled. Authorization remains enforced by Spring Security.

## Endpoints

### Search books

```http
GET /api/v1/books?q=java&page=0&size=20
```

Response fields include `items`, `page`, `totalPages`, and `totalItems`.

### Get a book

```http
GET /api/v1/books/{id}
```

Book responses include bibliographic information and the current count of available physical copies.

## Documentation

Authenticated administrators and librarians can open `/swagger-ui.html`. The OpenAPI document is available at `/v3/api-docs`.

## Conventions

- URLs are versioned under `/api/v1`.
- JPA entities are not exposed.
- Collection endpoints are paginated.
- Validation and authorization happen on the server.
- Future state-changing integration endpoints should support idempotency keys.
