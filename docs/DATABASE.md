# Database

Flyway owns schema changes. Hibernate runs with `ddl-auto=validate`; it never creates or silently changes production tables.

## Core relationships

```text
UserAccount 1---0..1 Member
Book *---* Author
Book *---* Category
Book 1---* BookCopy *---1 LibraryBranch
Member 1---* Loan *---1 BookCopy
Member 1---* Reservation *---1 Book
Member 1---* FineLedgerEntry
UserAccount 1---* Notification
```

## Key integrity rules

- User email, membership number, barcode, and accession number are unique.
- Each loan points to one physical copy and one member.
- Services lock the copy row during checkout and active-loan return.
- Active reservation duplicates are blocked by service rules.
- Financial history is append-only through normal application flows.
- Foreign keys protect historical circulation records.
- Timestamps are stored in UTC; due dates are date-only policy values.

## Migrations

Migrations live in `src/main/resources/db/migration` and must be forward-only, reviewed, and included in CI. Back up production before risky migrations. Do not edit an already deployed migration; add a new versioned migration.

## Backups

Production PostgreSQL should receive automated daily backups plus an off-host copy. Restore rehearsals are required; an untested backup is not considered a recovery plan.
