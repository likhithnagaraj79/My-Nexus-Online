import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/authStore'

const baseURL = import.meta.env.VITE_API_BASE_URL as string

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

export const apiClient = axios.create({
  baseURL,
  // No cookies anywhere in this backend — bearer tokens only.
  withCredentials: false,
})

// Separate instance for the refresh call itself, so its own 401s never re-trigger
// the response interceptor below (which would recurse).
const refreshClient = axios.create({ baseURL, withCredentials: false })

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  const { refreshToken, role, username } = useAuthStore.getState()
  if (!refreshToken || !role || !username) {
    throw new Error('No refresh token available')
  }
  const response = await refreshClient.post<{
    accessToken: string
    refreshToken: string
    mustChangePassword: boolean
  }>('/api/auth/refresh', { refreshToken })
  useAuthStore.getState().setTokens(response.data, role, username)
  return response.data.accessToken
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (!axios.isAxiosError(error) || !error.response) {
      return Promise.reject(error)
    }

    const { response, config } = error
    const requestConfig = config as RetriableRequestConfig | undefined
    const errorCode = (response.data as { errorCode?: string } | undefined)?.errorCode

    if (response.status === 403 && errorCode === 'PASSWORD_CHANGE_REQUIRED') {
      useAuthStore.getState().setMustChangePassword(true)
      if (window.location.pathname !== '/change-password') {
        window.location.assign('/change-password')
      }
      return Promise.reject(error)
    }

    if (response.status === 401 && requestConfig && !requestConfig._retried) {
      requestConfig._retried = true
      try {
        refreshPromise ??= refreshAccessToken()
        const newToken = await refreshPromise
        refreshPromise = null
        requestConfig.headers.set('Authorization', `Bearer ${newToken}`)
        return apiClient(requestConfig)
      } catch {
        refreshPromise = null
        useAuthStore.getState().logout()
        if (window.location.pathname !== '/login') {
          window.location.assign('/login')
        }
      }
    }

    return Promise.reject(error)
  },
)

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response) {
    const data = error.response.data as { detail?: string; title?: string } | undefined
    return data?.detail ?? data?.title ?? 'Something went wrong. Please try again.'
  }
  return 'Something went wrong. Please try again.'
}
