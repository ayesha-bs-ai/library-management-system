# Operations Runbook

## Health check

```bash
curl -f https://your-host/actuator/health
```

A healthy instance returns HTTP 200 and `{"status":"UP"}`.

## Start and stop

```bash
docker compose --profile full up -d --build
docker compose --profile full down
```

Do not use `down -v` in production because it removes the database volume.

## Logs

```bash
docker compose logs -f app
docker compose logs -f postgres
```

Application logs must not contain member secrets or passwords.

## Database backup

Example for a containerized PostgreSQL deployment:

```bash
docker compose exec -T postgres pg_dump -U library -Fc library > library-$(date +%F).dump
```

Copy backups off host and encrypt them at rest.

## Restore rehearsal

```bash
createdb library_restore_test
pg_restore --clean --if-exists --no-owner -d library_restore_test library-YYYY-MM-DD.dump
```

Start a staging application against the restored database and verify login, catalog counts, open loans, reservations, and ledger totals.

## Release procedure

1. Ensure CI passes on `main`.
2. Back up the database.
3. Review pending Flyway migrations.
4. Tag the release (`vX.Y.Z`).
5. Deploy the immutable GHCR image.
6. Confirm migrations and `/actuator/health`.
7. Run public catalog, staff login, checkout, return, and member portal smoke tests.
8. Monitor errors and database health.

## Incident priorities

- **P1:** Data loss, broad access-control bypass, unavailable circulation.
- **P2:** Incorrect fines/reservations, email outage, report failure.
- **P3:** Cosmetic or low-impact defects.

For P1, stop unsafe write traffic if necessary, preserve logs, take a database snapshot, communicate status, and restore only from a verified backup.
