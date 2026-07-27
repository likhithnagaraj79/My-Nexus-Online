import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest'
import { server } from './mocks/server'
import { useAuthStore } from '../store/authStore'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
beforeEach(() => {
  // Mirrors index.html's #badge-print-portal sibling of #root — ExhibitorPassesPage portals
  // its print content there, so anything rendering that component needs the target to exist.
  if (!document.getElementById('badge-print-portal')) {
    const portal = document.createElement('div')
    portal.id = 'badge-print-portal'
    document.body.appendChild(portal)
  }
})
afterEach(() => {
  cleanup()
  server.resetHandlers()
  useAuthStore.getState().logout()
  localStorage.clear()
  document.getElementById('badge-print-portal')?.remove()
})
afterAll(() => server.close())
