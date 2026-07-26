# Exhibitor's Registration System

Multi-role event exhibitor registration platform (Admin, Organiser, Crew, Validator, Public).

- `backend/` — Spring Boot (Java, Maven) API
- `frontend/` — React + TypeScript (Vite) web app
- `docs/spec/` — original product specification

See `backend/README.md` and `frontend/README.md` for stack-specific setup instructions.

## Local development prerequisites

- Java 21+ (JDK 26 via Homebrew is used on this machine — see `~/.zshrc` for `JAVA_HOME`)
- Node.js 20+ / npm
- PostgreSQL 16 and Redis, running as native Homebrew services (no Docker)

## Quick start

```bash
# Backend (serves HTTPS on https://localhost:8443 — see "HTTPS" below for first-time setup)
cd backend && SPRING_PROFILES_ACTIVE=dev,local ./mvnw spring-boot:run

# Frontend
cd frontend && npm run dev
```

Swagger UI: `https://localhost:8443/swagger-ui/index.html`

## HTTPS (local dev)

The backend serves HTTPS using a self-signed certificate that isn't committed. Generate it once:

```bash
mkdir -p backend/src/main/resources/keystore
keytool -genkeypair -alias exhibitor-dev -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore backend/src/main/resources/keystore/dev-keystore.p12 -validity 3650 \
  -dname "CN=localhost, OU=Dev, O=ExhibitorReg, L=Local, ST=Local, C=IN" \
  -storepass "<pick a password>" -keypass "<same password>"
```

Set that password as `SSL_KEYSTORE_PASSWORD` in `backend/src/main/resources/application-local.yml`
(copy from `application-local.yml.example`). Browsers/curl will warn about the self-signed
cert on localhost — expected; use `curl -k` or click through the browser warning.

## First Admin account

Set `ADMIN_BOOTSTRAP_USERNAME`/`ADMIN_BOOTSTRAP_PASSWORD` in `application-local.yml`. On first
startup, if the `users` table is empty, one Admin account is created automatically (must change
its password on first login). Every other account is created by that Admin via the API.
