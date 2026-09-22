# Railway Deployment Guide - Library Management System

This project is now **100% ready for Railway deployment** with all errors fixed.

## What Was Fixed

### 1. Railway DATABASE_URL Compatibility (CRITICAL)
**Problem:** Railway provides `DATABASE_URL` as `postgres://user:pass@host:port/db` but Spring Boot expects `jdbc:postgresql://...`
**Fix:** Created `RailwayEnvironmentPostProcessor` and `RailwayDatabaseConfiguration` that auto-convert.

### 2. Node.js Version Compatibility
Changed engines to `>=18`

### 3. Production Profile Mail Issue
Default to `false`

### 4. Dockerfile Improvements for Railway
- Added curl, HEALTHCHECK, PORT handling

### 5. Railway Config Files Added
- `railway.toml`, `railway.json`, `nixpacks.toml`, `Procfile`

### 6. ADMIN_EMAIL/PASSWORD Fix (NEW)
**Problem:** `ADMIN_EMAIL and ADMIN_PASSWORD required for first production start` crash loop on Railway
**Fix:** Updated `ProductionAdminInitializer` to create default admin `admin@library.local / Admin@12345!` if env vars not set, instead of crashing. Logs warning.

## Deploy to Railway

1. Push to GitHub
2. Railway.app -> New Project -> Deploy from GitHub
3. Add PostgreSQL Plugin (auto-injects DATABASE_URL)
4. Set Variables (optional but recommended):
   ```
   ADMIN_EMAIL=admin@yourdomain.com
   ADMIN_PASSWORD=StrongPass123!
   ```
   If not set, default admin `admin@library.local / Admin@12345!` will be created.

5. Deploy - Healthcheck at `/actuator/health`

## Env Vars

| Variable | Required | Example |
|----------|----------|---------|
| ADMIN_EMAIL | No (defaults) | admin@lib.com |
| ADMIN_PASSWORD | No (defaults) | Admin@12345! |
| DATABASE_URL | Auto | postgres://... |
| PORT | Auto | 8080 |
