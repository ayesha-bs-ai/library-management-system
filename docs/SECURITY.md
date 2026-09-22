# Security

## Implemented controls

- BCrypt adaptive password hashing
- Role-based URL authorization for admin, librarian, and member areas
- Ownership checks for member renewals, reservations, and notifications
- HTTP-only, same-site session cookies; secure cookies in production
- Session fixation protection and concurrent-session limit
- CSRF protection on all state-changing browser forms
- Forced password replacement for temporary accounts
- Server-side validation and Thymeleaf output escaping
- Transactional circulation operations and row locking
- Externalized production credentials
- Auditing for user, catalog, circulation, reservation, policy, and financial changes
- Safe error pages without production stack traces
- Development-only iframe allowance; production retains frame protection

## Deployment requirements

1. Terminate TLS at the platform ingress or reverse proxy.
2. Use a strong database password and a first-admin password of at least 12 characters.
3. Keep `dev` profile and demo credentials out of production.
4. Restrict actuator and database network access.
5. Configure SMTP credentials through secret storage.
6. Patch dependencies and review Dependabot alerts.
7. Back up and test restoration of PostgreSQL.
8. Review audit events and failed authentication logs.

## Secrets

Never commit `.env`, database passwords, SMTP credentials, session material, reset tokens, or access tokens. `.env.example` contains names only.

## Reporting vulnerabilities

Do not open a public issue containing an exploit or personal data. Contact the repository owner privately with reproduction steps and affected versions.
