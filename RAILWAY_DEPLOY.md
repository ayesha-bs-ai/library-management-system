# Railway Deployment Guide - Library Management System

This project is now **100% ready for Railway deployment** with all errors fixed.

## What Was Fixed

### 1. Railway DATABASE_URL Compatibility (CRITICAL)
**Problem:** Railway provides `DATABASE_URL` as `postgres://user:pass@host:port/db` but Spring Boot expects `jdbc:postgresql://...`
**Fix:** Created `RailwayEnvironmentPostProcessor` that:
- Auto-detects `DATABASE_URL` and `DATABASE_PRIVATE_URL`
- Converts `postgres://` to `jdbc:postgresql://` with `sslmode=require`
- Extracts username/password automatically
- Supports `JDBC_DATABASE_URL` as manual override
- Registered via `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`

### 2. Node.js Version Compatibility
**Problem:** `package.json` required Node >=22 but Railway default and many envs use Node 20
**Fix:** Changed engines to `>=18` (Tailwind 3.4.19 works on Node 18+)

### 3. Production Profile Mail Issue
**Problem:** `application-prod.yml` had `MAIL_ENABLED:true` by default, causing startup failure if no mail server
**Fix:** Default to `false`, with graceful fallback

### 4. Dockerfile Improvements for Railway
- Added `curl` for healthchecks
- Added `HEALTHCHECK` instruction
- Proper `PORT` handling: `ENTRYPOINT` uses `${PORT:-8080}`
- Added `JAVA_OPTS` with G1GC and MaxRAMPercentage
- Create `/app/data` directory
- Changed Node from 24-alpine to 20-alpine for better compatibility

### 5. Railway Config Files Added
- `railway.toml` - Tells Railway to use Dockerfile builder + healthcheck
- `railway.json` - Alternative JSON config
- `nixpacks.toml` - Fallback if Dockerfile builder not used, ensures JDK21 + Node20
- `Procfile` - For Heroku-style deployments
- `.env.railway.example` - All required env vars

### 6. Tailwind CSS Build
- Verified `npm run build:css` works
- CSS file is 38KB minified and committed

## Deploy to Railway - Step by Step

### Option A: Deploy via GitHub (Recommended)

1. **Push this fixed code to your GitHub repo**

2. **Go to Railway.app** -> New Project -> Deploy from GitHub -> Select your repo

3. **Add PostgreSQL Plugin:**
   - In Railway project, click "+ New" -> Database -> Add PostgreSQL
   - Railway will automatically inject `DATABASE_URL` and `DATABASE_PRIVATE_URL`
   - No manual DB config needed!

4. **Set Required Environment Variables:**
   In your app service -> Variables tab, add:
   ```
   ADMIN_EMAIL=admin@yourdomain.com
   ADMIN_PASSWORD=YourStrongPass123!
   ```
   Must be at least 12 characters.

5. **Optional Variables:**
   ```
   LIBRARY_NAME=My Library
   APP_BASE_URL=https://${{RAILWAY_PUBLIC_DOMAIN}}
   APP_CURRENCY=PKR
   ```

6. **Deploy:**
   Railway will:
   - Detect Dockerfile
   - Build Tailwind CSS (Stage 1)
   - Build Java app with Maven (Stage 2)
   - Run with Postgres (Stage 3)
   - Healthcheck at `/actuator/health`

7. **First Login:**
   - Go to your Railway public domain (e.g., `https://yourapp.up.railway.app`)
   - Login with ADMIN_EMAIL / ADMIN_PASSWORD
   - You'll be forced to change password on first login (security feature)
   - Then create librarian accounts via Admin panel

### Option B: Deploy via Railway CLI

```bash
npm i -g @railway/cli
railway login
railway init
railway add --plugin postgresql
railway variables set ADMIN_EMAIL=admin@example.com
railway variables set ADMIN_PASSWORD=SuperSecure123!
railway up
```

## Environment Variables Reference

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `ADMIN_EMAIL` | YES (first run) | Initial admin email | `admin@lib.com` |
| `ADMIN_PASSWORD` | YES (first run) | Min 12 chars | `Admin@12345!` |
| `DATABASE_URL` | Auto | Railway injects | `postgres://...` |
| `JDBC_DATABASE_URL` | No | Override if external DB | `jdbc:postgresql://...` |
| `PORT` | Auto | Railway injects | `8080` |
| `APP_BASE_URL` | No | Public URL | `https://app.up.railway.app` |
| `RAILWAY_PUBLIC_DOMAIN` | Auto | Railway injects | `app.up.railway.app` |
| `LIBRARY_NAME` | No | Branding | `Library Hub` |
| `MAIL_ENABLED` | No | Enable email | `false` |

## Testing Locally with Prod Profile (Simulates Railway)

```bash
# Set env vars
export DATABASE_URL=jdbc:postgresql://localhost:5432/library
export DATABASE_USERNAME=library
export DATABASE_PASSWORD=library_dev_password
export ADMIN_EMAIL=admin@test.com
export ADMIN_PASSWORD=Test12345!@#
export SPRING_PROFILES_ACTIVE=prod

./mvnw spring-boot:run
```

Or with Docker (like Railway does):

```bash
docker compose --profile full up --build
# Requires .env with DB_PASSWORD, ADMIN_EMAIL, ADMIN_PASSWORD
```

## Health Checks

Railway uses `/actuator/health` for healthchecks:
- Returns 200 when DB is connected and app is up
- No auth required (permitAll in SecurityConfig)
- Configured in both `railway.toml` and `Dockerfile`

## Troubleshooting

**App fails with "ADMIN_EMAIL required":**
- You didn't set ADMIN_EMAIL and ADMIN_PASSWORD in Railway Variables
- Set them and redeploy

**DB connection fails:**
- Ensure Postgres plugin is added and linked
- Check logs: our post-processor logs "[Railway] Resolved JDBC URL"
- If using external DB, set JDBC_DATABASE_URL directly

**Build fails on npm:**
- We fixed engines to >=18, should work on Node 20
- Dockerfile uses node:20-alpine

**CSS not loading:**
- Ensure `src/main/resources/static/css/app.css` exists (38KB)
- Dockerfile builds it in stage 1
- Run `npm run build:css` locally if needed

**Port issues:**
- Railway injects PORT, we respect it via `${PORT:-8080}`
- Don't hardcode 8080 in Railway settings

## Local Development (Still Works)

```bash
npm ci
npm run build:css
./mvnw spring-boot:run
# Open http://localhost:8080
# dev accounts: admin@library.local / Admin@12345
```

## Production Checklist

- [ ] Set strong ADMIN_PASSWORD (12+ chars)
- [ ] Set APP_BASE_URL to your Railway public domain
- [ ] Add custom domain in Railway if needed
- [ ] Enable MAIL_ENABLED only if you have SMTP
- [ ] Test backup/restore for Postgres plugin
- [ ] Change demo accounts after first login (they don't exist in prod)

## Files Changed/Added for Railway

- `src/main/java/.../RailwayEnvironmentPostProcessor.java` (NEW - critical fix)
- `src/main/resources/META-INF/spring/...imports` (NEW - registers processor)
- `src/main/resources/application-prod.yml` (FIXED - mail false, jdbc fallback)
- `package.json` (FIXED - node >=18)
- `Dockerfile` (IMPROVED - healthcheck, PORT, curl, node 20)
- `nixpacks.toml` (NEW)
- `railway.toml` (NEW)
- `railway.json` (NEW)
- `Procfile` (NEW)
- `.env.railway.example` (NEW)
- `RAILWAY_DEPLOY.md` (this file)

Ready to deploy! 🚀
