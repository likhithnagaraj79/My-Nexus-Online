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
# Backend
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm run dev
```
