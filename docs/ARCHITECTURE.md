# Architecture

## Style

The application is a package-by-feature modular monolith. One Spring Boot process owns business transactions and one relational database stores authoritative state. This keeps deployment simple while preserving boundaries between authentication, catalog, inventory, members, circulation, reservations, fines, notifications, reporting, and auditing.

## Request flow

```text
Browser / API client
        |
Spring Security filters
        |
MVC or REST controller
        |
Application service  <-- business rules and transaction boundary
        |
Spring Data repository
        |
PostgreSQL / H2
```

Controllers validate transport input and select views. Services enforce business rules. Repositories handle persistence. JPA entities are never returned directly from REST controllers.

## Important decisions

- Server-rendered Thymeleaf avoids a second frontend application and keeps the project Java-centered.
- Tailwind CSS is compiled locally; production does not depend on a CSS CDN.
- Sessions use HTTP-only cookies and CSRF protection.
- A title (`Book`) is separate from each physical `BookCopy`.
- Checkout and return operations are transactional.
- Pessimistic copy locking prevents concurrent double checkout.
- Loan records snapshot policy values so later policy changes do not rewrite history.
- Fines use immutable ledger entries rather than a mutable balance column.
- Reservation status is modeled explicitly and ready reservations can be assigned to a copy.
- Records with circulation history are archived or status-changed rather than cascade-deleted.

## Profiles

- `dev`: H2 file database, sample data, demo credentials, email disabled.
- `test`: isolated in-memory H2 database and no sample data.
- `prod`: PostgreSQL, secure cookies, environment-provided secrets and SMTP.

## UI design

The UI is Google/Material-inspired rather than a Google clone. It uses a search-first information hierarchy, large whitespace, rounded surfaces, soft elevation, clear focus states, and a restrained blue primary palette with small red, yellow, and green accents. All assets and styles are served locally.
