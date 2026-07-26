/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [react(), ...(command === 'serve' ? [basicSsl()] : [])],
  server: {
    port: 5173,
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    // Playwright owns e2e/**; Vitest's default include glob would otherwise also match its
    // *.spec.ts files and try (and fail) to run them under jsdom.
    exclude: ['e2e/**', 'node_modules/**'],
    env: {
      VITE_API_BASE_URL: 'https://localhost:8443',
      VITE_RECAPTCHA_SITE_KEY: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI',
    },
  },
}))
