import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/mocks/server'
import { useAuthStore } from '../store/authStore'
import { apiClient } from './client'

const BASE_URL = 'https://localhost:8443'

describe('apiClient interceptors', () => {
  beforeEach(() => {
    useAuthStore.getState().setTokens(
      { accessToken: 'expired-token', refreshToken: 'refresh-1', mustChangePassword: false },
      'ADMIN',
      'admin',
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('refreshes an expired access token and retries the original request once', async () => {
    server.use(
      http.get(`${BASE_URL}/api/test/protected`, ({ request }) => {
        const auth = request.headers.get('Authorization')
        if (auth === 'Bearer new-access-token') {
          return HttpResponse.json({ ok: true })
        }
        return HttpResponse.json({ title: 'Unauthorized', status: 401 }, { status: 401 })
      }),
      http.post(`${BASE_URL}/api/auth/refresh`, () =>
        HttpResponse.json({
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token',
          mustChangePassword: false,
        }),
      ),
    )

    const response = await apiClient.get('/api/test/protected')

    expect(response.data).toEqual({ ok: true })
    expect(useAuthStore.getState().accessToken).toBe('new-access-token')
  })

  it('redirects to /change-password on a 403 PASSWORD_CHANGE_REQUIRED response', async () => {
    server.use(
      http.get(`${BASE_URL}/api/test/needs-password-change`, () =>
        HttpResponse.json({ title: 'Forbidden', status: 403, errorCode: 'PASSWORD_CHANGE_REQUIRED' }, { status: 403 }),
      ),
    )
    const assign = vi.fn()
    const originalLocation = window.location
    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, assign },
      writable: true,
      configurable: true,
    })

    await expect(apiClient.get('/api/test/needs-password-change')).rejects.toBeDefined()

    expect(assign).toHaveBeenCalledWith('/change-password')
    Object.defineProperty(window, 'location', { value: originalLocation, writable: true, configurable: true })
    expect(useAuthStore.getState().mustChangePassword).toBe(true)
  })
})
