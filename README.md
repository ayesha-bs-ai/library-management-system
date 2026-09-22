# Library Management System

A complete Java web application for cataloging books, managing members, circulating physical copies, handling reservations and fines, and giving members a self-service portal.

The interface uses Tailwind CSS and a clean, search-first Material-inspired visual language with rounded surfaces, accessible colors, and responsive layouts.

## Features

### Public and member experience

- Search by title, author, ISBN, publisher, or category
- View title details and real-time copy availability
- Member dashboard with loans, due dates, renewals, reservations, fine ledger, and notifications
- Reserve available titles for pickup or join a waitlist
- Secure password-change flow for temporary accounts

### Librarian operations

- Catalog books, authors, categories, and individually barcoded copies
- Register and update members with optional portal accounts
- Fast barcode-focused checkout and return workflows
- Configurable renewals and transactional reservation fulfillment
- Automatic overdue charge calculation
- Manual payment and waiver ledger
- Reservation queue and pickup-expiry processing
- Operational dashboard, active-loan report, and CSV export

### Administration and platform

- Admin, librarian, and member role separation
- Staff account activation and suspension
- Configurable circulation, fine, and pickup policy
- Audit log for sensitive actions
- In-app and optional email notifications
- Flyway database migrations
- H2 development database and PostgreSQL production profile
- Docker image and Docker Compose services
- Health endpoints, OpenAPI/Swagger, tests, and GitHub Actions CI/CD

## Technology

- Java 21
- Spring Boot 3.5
- Spring MVC and Thymeleaf
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL, H2, and Flyway
- Tailwind CSS
- Maven and npm
- JUnit 5, Spring Boot Test, MockMvc, and Testcontainers support
- Docker and GitHub Actions

## Quick start

### Requirements

- Java 21
- Node.js 20 or newer

### Run locally

```bash
npm ci
npm run build:css
./mvnw spring-boot:run
```

Open <http://localhost:8080>.

The default development profile uses an H2 database stored under `data/`. It creates sample books, members, loans, a reservation, notifications, and safe demo accounts.

### Development accounts

| Role | Email | Password |
|---|---|---|
| Administrator | `admin@library.local` | `Admin@12345` |
| Librarian | `librarian@library.local` | `Library@12345` |
| Member | `member@library.local` | `Member@12345` |

These accounts exist only in the `dev` profile. Production requires administrator credentials through environment variables.

### Run tests

```bash
./mvnw test
```

### Watch Tailwind while editing templates

Run these in separate terminals:

```bash
npm run watch:css
./mvnw spring-boot:run
```

## PostgreSQL development services

Start PostgreSQL and Mailpit:

```bash
docker compose up -d postgres mailpit
```

Mailpit's local inbox is available at <http://localhost:8025>.

## Full production-like stack

Create a `.env` from `.env.example`, set strong credentials, and run:

```bash
docker compose --profile full up --build
```

Required production values include:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD` with at least 12 characters
- SMTP settings when email delivery is enabled

The application creates the first branch, default circulation policy, and administrator only when the production database is empty.

## API and health

Authenticated staff and members can access the versioned catalog API:

- `GET /api/v1/books`
- `GET /api/v1/books/{id}`

Staff can open Swagger UI at `/swagger-ui.html`.

Health endpoint:

- `GET /actuator/health`

## Main project structure

```text
src/main/java/com/librarymanagement
├── auth
├── audit
├── branch
├── catalog
├── circulation
├── fine
├── inventory
├── member
├── notification
├── reporting
└── reservation

src/main/resources
├── db/migration
├── static/css
├── static/js
└── templates
```

## Documentation

- [Implementation plan](docs/IMPLEMENTATION_PLAN.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Database](docs/DATABASE.md)
- [API](docs/API.md)
- [Security](docs/SECURITY.md)
- [Operations runbook](docs/RUNBOOK.md)

## License

No open-source license has been granted. All rights are reserved by default.
