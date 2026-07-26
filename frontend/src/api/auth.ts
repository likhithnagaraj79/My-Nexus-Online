import { apiClient } from './client'
import type { Role } from './types'

export interface LoginRequest {
  role: Role
  username: string
  password: string
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
  mustChangePassword: boolean
}

export interface LoginResponse {
  totpRequired: boolean
  loginTicketId: string | null
  tokens: TokenPair | null
}

export interface TotpLoginRequest {
  loginTicketId: string
  code: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export const login = (body: LoginRequest) =>
  apiClient.post<LoginResponse>('/api/auth/login', body).then((r) => r.data)

export const loginTotp = (body: TotpLoginRequest) =>
  apiClient.post<LoginResponse>('/api/auth/login/totp', body).then((r) => r.data)

export const logout = (refreshToken: string) =>
  apiClient.post<void>('/api/auth/logout', { refreshToken }).then((r) => r.data)

export const changePassword = (body: ChangePasswordRequest) =>
  apiClient.post<TokenPair>('/api/auth/change-password', body).then((r) => r.data)
