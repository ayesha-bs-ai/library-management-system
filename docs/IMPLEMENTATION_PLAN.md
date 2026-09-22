# Library Management System — End-to-End Implementation Plan

**Status:** Approved and executed as the initial production-ready release  
**Date:** 2026-09-21  
**Repository:** `ayesha-bs-ai/library-management-system`  
**Approach:** Java-based, production-ready modular monolith

---

## 1. Product Goal

Build a secure, responsive web application that manages the complete lifecycle of a physical library:

- Catalog books and individual physical copies
- Register and manage library members
- Issue, return, and renew books
- Reserve unavailable books and manage reservation queues
- Track due dates, overdue items, fines, and manual payments
- Notify members about due dates, overdue loans, and available reservations
- Give administrators and librarians operational dashboards and reports
- Give members a self-service portal for search, loans, renewals, reservations, and account history
- Maintain a complete audit trail of sensitive actions

The first release will be straightforward to run for one library while keeping the schema ready for multiple branches later.

---

## 2. Planning Assumptions

These defaults can be changed before implementation begins:

1. The first release serves one library location.
2. Branch identifiers are included in the data model so multi-branch support can be added without a major redesign.
3. The system manages physical books and copies; e-book DRM is outside the first release.
4. Roles are `ADMIN`, `LIBRARIAN`, and `MEMBER`.
5. Members have a self-service web portal.
6. Staff create member accounts; open public registration is disabled initially.
7. Fines are calculated automatically, while cash or externally completed payments are recorded manually.
8. The primary interface is English and responsive for desktop, tablet, and mobile.
9. Email notifications are included. SMS and WhatsApp are later enhancements.
10. The application is a modular monolith, not microservices, to reduce deployment and maintenance complexity.
11. PostgreSQL is the production database.
12. The application uses server-rendered Java views with small amounts of locally bundled JavaScript.

---

## 3. Recommended Technology Stack

### Application

- **Java:** Java 21 LTS
- **Framework:** Current supported stable Spring Boot release compatible with Java 21
- **Web:** Spring MVC
- **UI templates:** Thymeleaf
- **Styling:** Tailwind CSS, compiled and bundled locally
- **Visual direction:** Clean Google/Material-inspired search-first interface without copying Google branding
- **Small browser interactions:** Vanilla JavaScript; Node.js is used only to compile Tailwind CSS
- **Build:** Maven Wrapper plus npm scripts for the CSS pipeline

### Data and persistence

- **Database:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Schema migrations:** Flyway
- **Validation:** Jakarta Bean Validation
- **Concurrency:** Transaction boundaries, optimistic locking, and targeted row locking for circulation operations

### Security

- **Authentication and authorization:** Spring Security
- **Web authentication:** Secure server-side sessions using HTTP-only cookies
- **Password hashing:** Spring Security's supported adaptive password encoder
- **CSRF protection:** Enabled on state-changing browser requests
- **Authorization:** Role and ownership checks at URL and service layers

### Supporting services

- **Email:** Spring Mail
- **Development email inbox:** Mailpit through Docker Compose
- **Scheduled tasks:** Spring scheduling for reminders and overdue processing
- **API documentation:** Springdoc OpenAPI for selected REST endpoints
- **Health and metrics:** Spring Boot Actuator

### Testing and delivery

- **Unit tests:** JUnit 5 and Mockito
- **Integration tests:** Spring Boot Test and Testcontainers PostgreSQL
- **Browser tests:** Playwright for Java
- **Accessibility checks:** Automated checks plus manual keyboard testing
- **Local environment:** Docker Compose
- **Packaging:** Executable JAR and production Docker image
- **CI/CD:** GitHub Actions

---

## 4. Architecture

### Architectural style

Use a **package-by-feature modular monolith**. All modules run in one Spring Boot application and one database, but module boundaries are explicit. This gives simple operations now and leaves room to extract services only if real scaling needs appear.

```text
Browser
   |
   | HTTPS
   v
Spring Boot Application
   |-- Authentication and authorization
   |-- Catalog and inventory
   |-- Members
   |-- Circulation
   |-- Reservations
   |-- Fines and payments
   |-- Notifications
   |-- Reporting and audit
   |
   +--> PostgreSQL
   +--> Email provider / Mailpit
   +--> File or S3-compatible cover storage (optional)
```

### Feature modules

```text
com.librarymanagement
├── auth
├── user
├── branch
├── catalog
├── inventory
├── member
├── circulation
├── reservation
├── fine
├── notification
├── reporting
├── audit
└── common
```

Each module should normally contain its own:

- Web controllers
- REST controllers where needed
- Request and response models
- Application services
- Domain entities and rules
- Repositories
- Mappers
- Tests

Controllers must not contain business rules. Business transactions belong in application services.

---

## 5. Repository Structure

```text
library-management-system/
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── release.yml
├── docs/
│   ├── IMPLEMENTATION_PLAN.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── API.md
│   ├── SECURITY.md
│   └── RUNBOOK.md
├── src/
│   ├── main/
│   │   ├── java/com/librarymanagement/
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── static/
│   │       ├── templates/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/librarymanagement/
├── .env.example
├── .gitignore
├── compose.yml
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## 6. Users, Roles, and Permissions

### Public visitor

- View the public catalog
- Search by title, author, ISBN, category, and keyword
- See whether a title is available
- View library rules and opening information
- Sign in; no public self-registration in the first release

### Member

- Perform all public actions
- View profile and membership status
- View current and previous loans
- View due dates, overdue status, and fine balance
- Request eligible renewals
- Place and cancel reservations
- View reservation position and pickup expiry
- View notifications
- Change password

A member can access only their own private data.

### Librarian

- Search the catalog and scan barcodes
- Add and edit books, authors, categories, and copies
- Register and update members
- Issue, return, and renew books
- Manage reservations and ready-for-pickup items
- Record copy condition, loss, or damage
- Review fines and record manual payments or waivers within policy
- Run day-to-day reports

### Administrator

- Perform all librarian actions
- Manage staff accounts and role assignments
- Configure circulation and fine policies
- Configure branch and library details
- View security and audit events
- Export administrative reports
- Manage system configuration
- Archive records according to retention rules

### Permission principle

Use least privilege. Hiding a button in the UI is not authorization; every protected action must also be checked on the server.

---

## 7. Functional Modules

### 7.1 Authentication and account security

- Login and logout
- Password reset using short-lived, single-use tokens
- Forced password change for staff-created accounts
- Account activation, suspension, and lockout
- Configurable failed-login throttling
- Session timeout and logout from active sessions
- Role-based access control
- Member ownership checks
- Optional two-factor authentication as a later hardening feature

### 7.2 Catalog management

Separate a **book/title record** from its individual **physical copies**.

Book/title functions:

- ISBN-10 and ISBN-13
- Title, subtitle, edition, language, publication year, and description
- Multiple authors
- Publisher
- Multiple categories and keywords
- Cover image
- Dewey/classification number if used
- Soft archive rather than unsafe deletion when historical loans exist
- Duplicate detection by ISBN and edition

Catalog search:

- Title, author, ISBN, category, and keyword
- Availability filter
- Pagination and sorting
- Search designed initially with PostgreSQL indexing
- Dedicated search engine deferred until usage proves it necessary

### 7.3 Copy and inventory management

Each physical copy includes:

- Unique accession number
- Unique scannable barcode
- Book/title reference
- Owning branch
- Shelf/location code
- Acquisition date and source
- Purchase price if recorded
- Condition
- Current status
- Version field for concurrency protection

Copy statuses:

- `AVAILABLE`
- `ON_LOAN`
- `RESERVED`
- `LOST`
- `DAMAGED`
- `REPAIR`
- `WITHDRAWN`

Inventory operations:

- Add one or many copies
- Print barcode labels
- Scan a barcode into circulation screens
- Mark lost, damaged, repaired, or withdrawn
- Inventory reconciliation report
- Copy history timeline

### 7.4 Member management

- Unique membership number and optional barcode
- Name, email, phone, address, and date of birth where policy requires it
- Member type, such as student, teacher, or general member
- Join date and membership expiry
- Active, suspended, expired, or closed status
- Guardian/contact support for minors if required later
- Notes visible only to authorized staff
- Current loans, reservations, fines, and history
- Data export and archival process

### 7.5 Circulation policy engine

Rules must be configuration, not scattered constants.

Policy fields:

- Member type
- Loan duration
- Maximum simultaneous loans
- Maximum renewals
- Renewal extension
- Grace period
- Fine per overdue day
- Maximum fine per loan
- Fine balance that blocks borrowing
- Reservation pickup period
- Membership-expiry behavior

The first version can use one default policy plus optional member-type overrides.

### 7.6 Checkout

Checkout is one database transaction.

Validation order:

1. Librarian is authorized.
2. Member exists and is active.
3. Membership is not expired.
4. Member is not blocked by policy.
5. Loan limit has not been reached.
6. Copy exists and is in an issuable state.
7. Reservation rules allow this member to receive the copy.
8. A due date is calculated from the applicable policy.
9. Loan and audit records are stored.
10. Copy status changes to `ON_LOAN`.
11. A receipt or confirmation is displayed and optionally emailed.

A row lock or equivalent concurrency control must prevent two librarians from issuing the same copy.

### 7.7 Return

Return is one database transaction.

1. Find the open loan by barcode.
2. Record return timestamp and receiving librarian.
3. Calculate overdue days and any fine from the loan's policy snapshot.
4. Record copy condition.
5. If a reservation queue exists, mark the copy for the next member.
6. Otherwise mark the copy `AVAILABLE`.
7. Add fine ledger entries when applicable.
8. Add audit and notification events.
9. Display or print a return receipt.

Policy values that affect an active loan should be snapshotted so later policy changes do not rewrite history.

### 7.8 Renewal

A renewal is allowed only when:

- The loan is still open
- The member is active and eligible
- The maximum renewal count is not reached
- No eligible member is waiting for the title
- The item is not marked lost or damaged
- The resulting due date does not violate policy

Both staff and members can request renewal, with the same service-layer rules.

### 7.9 Reservations and hold queue

- Reservations are placed at title level
- One active reservation per member per title
- FIFO queue, with an administrative override recorded in the audit log
- On return, the first eligible reservation becomes `READY_FOR_PICKUP`
- Pickup expiry is calculated from policy
- Expired reservations move to the next eligible member
- Members can cancel reservations
- Notification events are generated for ready and expired reservations

Reservation statuses:

- `WAITING`
- `READY_FOR_PICKUP`
- `FULFILLED`
- `CANCELLED`
- `EXPIRED`

### 7.10 Fines and payment ledger

Use a ledger rather than a single mutable balance.

Entry types:

- Overdue charge
- Lost-item charge
- Damage charge
- Manual adjustment
- Waiver
- Payment
- Refund or correction

Requirements:

- Every adjustment records actor, timestamp, reason, and related loan when applicable
- Payments are manually recorded in the first release
- Receipts have unique references
- The current balance is derived from immutable entries
- Deleting financial history is not allowed through normal UI flows
- Online payment integration remains a later phase

### 7.11 Notifications

Channels in the first release:

- In-app notification center
- Email

Events:

- Account created
- Password reset
- Checkout receipt
- Due soon
- Overdue
- Reservation ready
- Reservation expiring
- Membership expiring

Implementation approach:

- Save a notification/outbox record in the business transaction
- Process queued notifications asynchronously after commit
- Retry temporary failures
- Record delivery status and error details without exposing secrets
- Scheduled jobs create due-soon and overdue events once per applicable period

### 7.12 Dashboards and reports

Librarian dashboard:

- Loans issued today
- Returns today
- Currently overdue loans
- Reservations awaiting pickup
- Expiring pickups
- Inventory alerts

Administrator reports:

- Current loans
- Overdue loans
- Popular titles
- Circulation by date and category
- Active and expired members
- Lost and damaged copies
- Fine charges, payments, and waivers
- Inventory valuation where purchase data exists
- Staff activity and audit reports

Exports:

- CSV for all tabular reports
- PDF only where a formatted printable document adds value
- Export permission and action must be audited

### 7.13 Audit trail

Audit important actions, including:

- Login successes and failures
- Account and role changes
- Member status changes
- Book and copy changes
- Checkouts, returns, renewals, and overrides
- Reservation queue overrides
- Fine adjustments, waivers, and payments
- Report exports
- Configuration changes

Audit entries should contain actor, action, target type, target identifier, timestamp, request/correlation identifier, and safe before/after details. Passwords, session IDs, reset tokens, and other secrets must never be logged.

---

## 8. Core Data Model

### Main entities

| Entity | Purpose |
|---|---|
| `LibraryBranch` | Library location; one active branch initially |
| `UserAccount` | Login identity and account state |
| `Role` | Administrative, librarian, or member authority |
| `Member` | Library membership and borrowing status |
| `MemberType` | Policy grouping such as student or general |
| `Book` | Bibliographic/title record |
| `Author` | Author identity |
| `BookAuthor` | Ordered many-to-many book-author link |
| `Publisher` | Publisher record |
| `Category` | Catalog classification |
| `BookCategory` | Many-to-many book-category link |
| `BookCopy` | Individually tracked physical copy |
| `CirculationPolicy` | Loan, renewal, reservation, and fine rules |
| `Loan` | One checkout lifecycle for one copy |
| `Renewal` | Renewal history for a loan |
| `Reservation` | Member's title-level hold and queue position |
| `FineLedgerEntry` | Charge, payment, waiver, or correction |
| `Notification` | In-app/email event and delivery state |
| `AuditEvent` | Immutable record of a sensitive action |
| `PasswordResetToken` | Hashed, expiring, single-use reset token |

### Important database constraints

- Unique normalized ISBN where supplied and appropriate for edition handling
- Unique accession number
- Unique copy barcode
- Unique member number
- Unique normalized user email
- At most one open loan per physical copy
- At most one active reservation per member per title
- Nonnegative renewal count and policy limits
- Foreign-key integrity for all history
- Version columns on concurrency-sensitive records
- Timestamps stored in UTC and displayed in the configured library timezone

### Deletion policy

- Use archive/status changes for books, members, users, and copies with history
- Never cascade-delete loan, fine, payment, or audit history
- Apply a documented retention policy to personal data

A formal ER diagram and migrations will be produced before feature implementation.

---

## 9. Web Pages and User Experience

### Public pages

- Home and library information
- Catalog search and filters
- Book detail and availability
- Login and password reset

### Member pages

- Dashboard
- Current loans and due dates
- Loan history
- Reservations and queue status
- Fine ledger and balance
- Notifications
- Profile and password settings

### Staff pages

- Operations dashboard
- Fast checkout screen with barcode focus
- Fast return screen with barcode focus
- Book and author management
- Copy and inventory management
- Member search, registration, and profile
- Reservations and pickup queue
- Fine/payment recording
- Reports and exports

### Administrator pages

- Staff and roles
- Circulation policies
- Library/branch configuration
- Notification templates
- Audit explorer
- System health summary without exposing secrets

### UX requirements

- Responsive layout
- Full keyboard navigation for circulation desks
- Barcode scanner support as keyboard input
- Clear confirmation before irreversible or financial operations
- Friendly validation errors that preserve safe form input
- Empty states, loading states, and error states
- Accessible labels, contrast, focus indicators, and semantic HTML
- No color-only status communication

---

## 10. API Plan

The primary UI is server-rendered, but selected versioned JSON endpoints will support future clients and integrations.

Example resource groups:

```text
/api/v1/auth
/api/v1/books
/api/v1/copies
/api/v1/members
/api/v1/loans
/api/v1/reservations
/api/v1/fines
/api/v1/reports
```

API rules:

- Version from the start
- Use request/response DTOs, never expose JPA entities directly
- Consistent validation and error format
- Pagination for collection endpoints
- OpenAPI documentation
- Role and ownership checks identical to web flows
- Idempotency protection where repeated requests could create duplicate financial or circulation actions
- Avoid implementing endpoints that have no real first-release consumer

---

## 11. Security Plan

### Required controls

- HTTPS in staging and production
- Secure, HTTP-only, same-site cookies
- CSRF protection
- Adaptive password hashing
- Password reset tokens stored only as hashes
- Login throttling and temporary lockout
- Server-side authorization on every protected operation
- Input validation and output escaping
- Prepared ORM queries; no unsafe SQL construction
- Safe file type and size validation for uploads
- Security headers, including Content Security Policy
- Secrets supplied through environment or secret storage, never committed
- Separate development, test, staging, and production configuration
- Audit of privileged and financial operations
- Dependency and container scanning in CI
- Regular backup and restore verification

### Privacy

- Collect only data needed for library operations
- Restrict sensitive member fields to authorized staff
- Export personal data only with permission and audit logging
- Document retention and anonymization procedures
- Never include private member data in application logs

### Threat review before release

Perform a focused review for:

- Broken access control
- Member-to-member data leakage
- CSRF and session fixation
- Injection
- Stored cross-site scripting in catalog or notes fields
- Brute-force login attempts
- Insecure direct object references
- Duplicate checkout/payment submissions
- Malicious file uploads
- Sensitive data in logs, exports, or backups

---

## 12. Reliability and Performance

### Initial targets

- Common pages respond within two seconds under normal expected load
- Search and circulation operations use indexed database queries
- Checkout and return remain transactionally consistent during concurrent staff use
- Pagination is mandatory for large lists
- Reports run with bounded date ranges or through background export jobs when expensive
- Application starts and becomes healthy automatically after deployment

### Data protection

- Automated PostgreSQL backups
- At least one off-host backup copy in production
- Documented restore procedure
- Periodic restore test
- Database migrations are forward-only in production and backed up before risky changes

### Observability

- Structured application logs
- Correlation/request IDs
- Spring Boot health/readiness endpoints
- Metrics for request errors, latency, login failures, email failures, scheduled jobs, and database health
- Alerts for repeated failures and storage or database issues
- No secrets or unnecessary personal data in telemetry

---

## 13. Testing Strategy

### Unit tests

Test pure business rules:

- Due-date calculation
- Fine calculation and caps
- Loan eligibility
- Renewal eligibility
- Reservation ordering
- Membership expiry rules
- Permission decisions

### Repository and integration tests

Use Testcontainers with real PostgreSQL behavior for:

- Constraints and indexes
- JPA mappings
- Flyway migrations
- Transaction boundaries
- Concurrent checkout protection
- Full service workflows
- Security configuration

### Web/controller tests

- Authentication and access rules
- Validation errors
- CSRF enforcement
- Form and API responses
- Member ownership restrictions

### End-to-end browser tests

Critical paths:

1. Administrator creates librarian
2. Librarian creates book and copy
3. Librarian registers member
4. Librarian checks out copy
5. Member views loan
6. Member renews eligible loan
7. Librarian returns copy
8. Fine is calculated when overdue
9. Librarian records payment
10. Reservation moves through waiting, pickup, and fulfillment

### Additional quality checks

- Maven formatting/style rules
- Static analysis
- Dependency vulnerability scanning
- Container scanning
- Accessibility checks
- Responsive browser checks
- Backup and restore rehearsal
- Basic load test for search and circulation

### Coverage principle

Use coverage as a signal, not the goal. Require high coverage for policy and financial calculations and strong workflow coverage for circulation and authorization.

---

## 14. Local Development and Environments

### Local development

A single command should start required infrastructure:

```bash
docker compose up -d postgres mailpit
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Development setup includes:

- PostgreSQL
- Mailpit
- Safe seed data
- No fixed production-like passwords in source control
- `.env.example` documenting required variables

### Environments

- **Development:** Local services and generated sample data
- **Test:** Disposable Testcontainers databases
- **Staging:** Production-like deployment with non-production data
- **Production:** Managed secrets, HTTPS, backups, monitoring, and restricted administration

### Deployment shape

- One application container
- One PostgreSQL database
- Reverse proxy or managed platform ingress
- SMTP/email provider
- Persistent/object storage only if cover uploads are enabled

The deployment target can be chosen later without changing the application architecture.

---

## 15. CI/CD Plan

### Pull-request workflow

GitHub Actions should:

1. Check formatting and style
2. Compile the application
3. Run unit tests
4. Run PostgreSQL integration tests
5. Run security/dependency checks
6. Build the JAR
7. Build the Docker image
8. Publish test reports

A failing required check blocks merge.

### Main-branch workflow

1. Repeat all required checks
2. Produce a versioned artifact and image
3. Deploy automatically to staging
4. Run smoke tests
5. Require approval for production
6. Deploy immutable image to production
7. Run database migration and health checks
8. Keep a rollback procedure for application images

Database rollback relies primarily on backups and carefully designed forward migrations rather than unsafe automatic down-migrations.

---

## 16. Delivery Phases

Estimates are approximate for one developer and should be revised after scope approval.

### Phase 0 — Scope and design approval (1–2 days)

Deliverables:

- Approved assumptions and exclusions
- Role/permission matrix
- User-flow sketches
- ER diagram
- Architecture decision records
- Prioritized backlog

Exit criteria:

- No unresolved decision that changes the core schema or authentication model

### Phase 1 — Project foundation (2–3 days)

Deliverables:

- Spring Boot/Maven project
- Package-by-feature structure
- Profiles and configuration validation
- PostgreSQL and Flyway
- Docker Compose
- Common error handling and validation
- Base UI layout and design tokens
- CI pipeline and initial tests

Exit criteria:

- Clean checkout builds and tests successfully
- Local application starts using documented commands

### Phase 2 — Authentication, users, and authorization (3–4 days)

Deliverables:

- Login/logout
- Staff-created accounts
- Roles and permission checks
- Password reset
- Account status and lockout
- Audit foundation
- Security tests

Exit criteria:

- Each role can access only its permitted screens and data

### Phase 3 — Catalog, copies, and members (5–7 days)

Deliverables:

- Book, author, publisher, and category management
- Copy and barcode management
- Catalog search
- Member registration and management
- Public and member catalog views
- Imports/exports only if approved for first release

Exit criteria:

- Staff can create a complete title, add copies, register a member, and find both efficiently

### Phase 4 — Core circulation (5–7 days)

Deliverables:

- Circulation policies
- Checkout
- Return
- Renewal
- Copy/member status enforcement
- Receipts
- Concurrency protection
- Complete circulation audit history

Exit criteria:

- Critical checkout/return/renewal browser tests pass
- The same copy cannot be issued twice

### Phase 5 — Reservations, fines, and notifications (5–7 days)

Deliverables:

- Reservation queue
- Ready-for-pickup and expiry workflow
- Fine ledger and calculation
- Manual payment/waiver records
- In-app and email notifications
- Reminder/overdue scheduled jobs

Exit criteria:

- Reservation and financial histories are correct and auditable

### Phase 6 — Dashboards, reports, and hardening (4–6 days)

Deliverables:

- Staff and administrator dashboards
- Operational and financial reports
- CSV exports
- Query/index tuning
- Accessibility improvements
- Security review fixes
- Observability and alert hooks

Exit criteria:

- Agreed reports reconcile with database records
- No known critical security or accessibility issue remains

### Phase 7 — Release readiness and deployment (2–4 days)

Deliverables:

- Production Docker image
- Staging deployment
- Backup and restore runbook
- Administrator and librarian guides
- Seed/setup procedure for the first administrator
- End-to-end and smoke test suite
- Production deployment checklist

Exit criteria:

- Fresh deployment, migration, backup, restore, and smoke tests succeed

### Overall estimate

Approximately **5–7 weeks for one developer** for a carefully tested first production release. A reduced MVP can be demonstrated earlier after Phase 4.

---

## 17. Proposed GitHub Epics

1. `EPIC-01` Project foundation and developer experience
2. `EPIC-02` Authentication and role-based authorization
3. `EPIC-03` Catalog and bibliographic records
4. `EPIC-04` Physical-copy inventory
5. `EPIC-05` Member management
6. `EPIC-06` Circulation policies
7. `EPIC-07` Checkout, return, and renewal
8. `EPIC-08` Reservations and pickup queue
9. `EPIC-09` Fine and payment ledger
10. `EPIC-10` Notifications and scheduled jobs
11. `EPIC-11` Dashboards, reports, and exports
12. `EPIC-12` Audit, security, and privacy
13. `EPIC-13` Testing and quality assurance
14. `EPIC-14` Deployment, monitoring, backup, and documentation

Each feature issue should contain:

- User story or operational goal
- Business rules
- Acceptance criteria
- Permission requirements
- Test expectations
- Dependencies
- UI/API notes
- Migration impact where applicable

---

## 18. First-Release Acceptance Criteria

The first release is complete only when:

1. A new environment can be started from documented steps.
2. The first administrator is created securely without a committed default password.
3. An administrator can create and suspend staff accounts.
4. A librarian can create a book and one or more uniquely barcoded copies.
5. A librarian can register and manage a member.
6. A librarian can issue, return, and renew eligible copies.
7. The application prevents double checkout and invalid state transitions.
8. A member can search the catalog and see only their own account activity.
9. Reservations are queued and fulfilled correctly.
10. Overdue fines follow the configured policy and produce immutable ledger entries.
11. Authorized staff can record payments and waivers with reasons.
12. Email/in-app notifications are queued, retried, and traceable.
13. Reports and exports enforce permissions and are audited.
14. Critical workflows have unit, integration, security, and browser tests.
15. CI passes on the release commit.
16. Production uses HTTPS, externalized secrets, backups, and health monitoring.
17. Backup restoration has been tested.
18. Operator and user documentation is available.
19. No known critical or high-severity security defect remains.
20. A staging smoke test passes before production deployment.

---

## 19. Explicitly Deferred Features

These should not delay the initial release unless requirements change:

- Native Android or iOS applications
- E-book hosting or DRM
- RFID gates and RFID readers
- SIP2 integration with self-check machines
- Inter-library loans
- Supplier acquisition and purchase-order workflows
- Online payment gateway
- SMS or WhatsApp delivery
- Recommendation engine
- Full multi-branch transfer workflows
- Multi-tenant hosting for unrelated libraries
- Elasticsearch/OpenSearch
- Microservices
- Offline circulation mode

The architecture should not block later integrations, but speculative complexity will not be built into the MVP.

---

## 20. Key Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Duplicate checkout during concurrent use | Database transaction, copy locking, unique open-loan rule, concurrency test |
| Fine disagreements after policy changes | Snapshot applicable policy values on each loan |
| Unauthorized member-data access | Service-layer ownership checks and negative security tests |
| Accidental deletion of history | Archive records; prohibit destructive cascades |
| Notification failure | Transactional outbox, retries, delivery status, monitoring |
| Slow catalog/report queries | Pagination, indexes, query review, bounded exports |
| Scope growth | Approved first-release scope and deferred-feature list |
| Deployment configuration drift | Containers, environment validation, CI-built immutable artifacts |
| Backup that cannot be restored | Scheduled restore rehearsals and documented runbook |
| Sensitive data in logs | Structured safe logging, review, and tests for secret handling |

---

## 21. Definition of Done for Every Feature

A feature is done only when:

- Business rules and acceptance criteria are satisfied
- Server-side authorization is implemented
- Validation and error handling are included
- Database migration is reviewed when applicable
- Unit and integration tests pass
- A browser test covers the feature when it is a critical user path
- Accessibility and responsive behavior are checked
- Audit logging is included for sensitive actions
- No secrets or sensitive personal data are logged
- Documentation is updated
- CI passes
- Code is reviewed and merged through a pull request

---

## 22. Approval Gate Before Coding

Before application implementation starts, confirm or revise these five choices:

1. **UI:** Spring MVC + Thymeleaf, rather than a separate React frontend
2. **Branch scope:** One library initially, with branch-ready entities
3. **Portals:** Admin, librarian, member, and public catalog
4. **Fines:** Automatic calculation with manually recorded payments; no online gateway initially
5. **Deployment:** Dockerized and cloud-neutral until a hosting target is selected

After approval, the first implementation step will be **Phase 1: project foundation**, followed by a running skeleton application and CI tests. No domain feature should be started before the foundation and schema design are reviewed.
