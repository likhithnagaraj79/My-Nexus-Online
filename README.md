# Exhibitor's Registration System

Multi-role event exhibitor registration platform (Admin, Organiser, Crew, Validator, Public).

- `backend/` — Spring Boot (Java, Maven) API
- `frontend/` — React + TypeScript (Vite) web app
- `docs/spec/` — original product specification
- `docs/PRODUCTION.md` — production deployment guide (no Docker; plain `java -jar` + a static
  frontend build behind a reverse proxy)

See `backend/README.md` and `frontend/README.md` for stack-specific setup instructions.

## Local development prerequisites

- Java 21+ (JDK 26 via Homebrew is used on this machine — see `~/.zshrc` for `JAVA_HOME`)
- Node.js 20+ / npm
- PostgreSQL 16 and Redis, running as native Homebrew services (no Docker)

## Quick start

```bash
# Backend (serves HTTPS on https://localhost:8443 — see "HTTPS" below for first-time setup)
cd backend && SPRING_PROFILES_ACTIVE=dev,local ./mvnw spring-boot:run

# Frontend (serves HTTPS on https://localhost:5173 — required to match the backend's CORS allowlist)
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

The frontend's Vite dev server also serves HTTPS, via `@vitejs/plugin-basic-ssl`
(auto-generates its own self-signed cert, no setup needed — this only runs for `npm run dev`,
not `npm run build`). This is required: the backend's CORS allowlist (`app.cors.allowed-origins`)
is pinned to `https://localhost:5173`, so a plain-HTTP dev server would be rejected by the
browser's CORS preflight. Expect a second, separate self-signed-cert warning the first time you
open `https://localhost:5173`.

## First Admin account

Set `ADMIN_BOOTSTRAP_USERNAME`/`ADMIN_BOOTSTRAP_PASSWORD` in `application-local.yml`. On first
startup, if the `users` table is empty, one Admin account is created automatically (must change
its password on first login). Every other account is created by that Admin via the API.

## Testing

- `cd backend && ./mvnw test` — unit + integration tests (H2 + an in-memory Redis fake), safe to
  run anywhere, no external services required beyond what Maven already resolves.
- `cd frontend && npm test` — component tests (Vitest + Testing Library + msw, network-mocked).
- `cd frontend && npm run test:e2e` — full real-browser end-to-end suite (Playwright), driving the
  actual golden path (bootstrap Admin → create Organiser/Crew/Validator → event setup → public
  registration → Crew print/issue → Validator scan → Organiser export) against real local
  Postgres/Redis and the real dev servers. One-time setup:
  - `npx playwright install --with-deps chromium` (downloads browser binaries)
  - `createdb exhibitor_registration_e2e -O exhibitor_app` (a dedicated, disposable database —
    reset automatically before every run via the `pretest:e2e` npm hook; `exhibitor_app` has no
    `CREATEDB` grant, so this one-time creation needs your own OS-superuser Postgres role)
  - **Stop any manually running `mvnw spring-boot:run` / `npm run dev` first** — the E2E suite
    starts its own backend+frontend on the same ports (8443/5173) and will fail with `EADDRINUSE`
    if something is already listening there.
- `./scripts/test-all.sh` from the repo root — runs all three suites above in order, stopping at
  the first failure. There's no CI configured yet, so this is the "run everything" entry point.

### Opt-in: backup/restore integration test

`BackupRestoreRealPostgresIntegrationTest` shells out to the real `pg_dump`/`pg_restore`
binaries against a real Postgres database, so it's skipped by default `mvn test` (environments
without those tools on `PATH` would otherwise break). One-time setup, same reasoning as the E2E
database above:

```bash
createdb exhibitor_registration_backup_it -O exhibitor_app
```

Then run it explicitly:

```bash
RUN_PG_BACKUP_IT=true DB_USERNAME=exhibitor_app DB_PASSWORD=<your local exhibitor_app password> \
  ./mvnw test -Dtest=BackupRestoreRealPostgresIntegrationTest
```

## Production

See [`docs/PRODUCTION.md`](docs/PRODUCTION.md) — no Docker; the backend runs as a plain
`java -jar` process behind a reverse proxy that terminates TLS, and the frontend is a static
build served by any web server. Covers the required environment variables, a reverse-proxy
example, and a pre-deploy security checklist.
