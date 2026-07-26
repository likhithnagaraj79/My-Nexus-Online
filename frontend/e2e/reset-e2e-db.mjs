// Drops + recreates the exhibitor_registration_e2e Postgres database so every E2E run starts
// from a clean, freshly-migrated schema. Runs as an npm "pretest:e2e" hook — deliberately NOT
// inside Playwright's globalSetup, because Playwright starts webServer processes (this backend
// included) *before* globalSetup runs (confirmed by reading playwright's runner source,
// createGlobalSetupTasks — plugin/webServer setup precedes globalSetup tasks). Resetting the DB
// from globalSetup would drop it out from under an already-started backend's connection pool.
//
// Connects to the "postgres" maintenance database via trust-authenticated superuser access
// (this machine's pg_hba.conf allows local/loopback trust — see README). Parameterized via env
// vars for portability rather than hardcoded, even though sensible localhost defaults work here.
import pg from 'pg'

const DB_NAME = 'exhibitor_registration_e2e'
const APP_ROLE = process.env.E2E_PG_APP_ROLE ?? 'exhibitor_app'

const client = new pg.Client({
  host: process.env.E2E_PG_HOST ?? 'localhost',
  port: Number(process.env.E2E_PG_PORT ?? 5432),
  user: process.env.E2E_PG_ADMIN_USER ?? process.env.USER,
  password: process.env.E2E_PG_ADMIN_PASSWORD ?? undefined,
  database: 'postgres',
})

await client.connect()
try {
  console.log(`[reset-e2e-db] Dropping database "${DB_NAME}" (if it exists)...`)
  await client.query(`DROP DATABASE IF EXISTS ${DB_NAME} WITH (FORCE)`)
  console.log(`[reset-e2e-db] Creating database "${DB_NAME}" owned by "${APP_ROLE}"...`)
  await client.query(`CREATE DATABASE ${DB_NAME} OWNER ${APP_ROLE}`)
  console.log('[reset-e2e-db] Done.')
} finally {
  await client.end()
}
