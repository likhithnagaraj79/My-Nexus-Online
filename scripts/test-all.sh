#!/usr/bin/env bash
# Runs every test suite in the repo, in order, stopping at the first failure.
# There's no CI configured yet, so this is the "run everything" entry point.
#
# Requires: JAVA_HOME set (see backend/README.md), native Postgres/Redis running, and
# manual `mvnw spring-boot:run` / `npm run dev` dev servers NOT already running (the E2E
# suite starts its own and will fail with EADDRINUSE otherwise).
set -e

cd "$(dirname "$0")/.."

echo "==> Backend: mvn test"
(cd backend && ./mvnw test)

echo "==> Frontend: unit tests (vitest)"
(cd frontend && npm test)

echo "==> Frontend: end-to-end tests (playwright)"
(cd frontend && npm run test:e2e)

echo "==> All test suites passed."
