import { defineConfig } from '@playwright/test'

// Deliberately separate from vite.config.ts's Vitest `test` block: `npm test` (Vitest) only
// looks at src/**/*.test.tsx, and this config's testDir keeps Playwright specs out of that
// picture entirely — neither runner ever picks up the other's spec files.
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'https://localhost:5173',
    ignoreHTTPSErrors: true,
    trace: 'retain-on-failure',
  },
  webServer: [
    {
      command: './mvnw spring-boot:run',
      cwd: '../backend',
      url: 'https://localhost:8443/actuator/health',
      ignoreHTTPSErrors: true,
      timeout: 120_000,
      // Must be false: the Playwright default (true when no CI env var is set, which is the
      // case here) would silently reuse a developer's already-running manual dev server
      // against the real dev database instead of the freshly-reset e2e one. Stop any manual
      // `mvnw spring-boot:run` / `npm run dev` before running `npm run test:e2e`.
      reuseExistingServer: false,
      env: {
        ...process.env,
        JAVA_HOME: process.env.JAVA_HOME ?? '/opt/homebrew/opt/openjdk',
        SPRING_PROFILES_ACTIVE: 'dev,local,e2e',
      },
    },
    {
      command: 'npm run dev',
      cwd: '.',
      url: 'https://localhost:5173',
      ignoreHTTPSErrors: true,
      timeout: 60_000,
      reuseExistingServer: false,
    },
  ],
})
