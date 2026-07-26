import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import type { Role } from '../api/types'
import type { TokenPair } from '../api/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  role: Role | null
  username: string | null
  mustChangePassword: boolean
  /** Transient — set between POST /login and POST /login/totp, never persisted. */
  pendingTotpTicket: string | null

  setTokens: (tokens: TokenPair, role: Role, username: string) => void
  setPendingTotp: (loginTicketId: string, role: Role, username: string) => void
  setMustChangePassword: (value: boolean) => void
  logout: () => void
}

const initialState = {
  accessToken: null,
  refreshToken: null,
  role: null,
  username: null,
  mustChangePassword: false,
  pendingTotpTicket: null,
} satisfies Omit<AuthState, 'setTokens' | 'setPendingTotp' | 'setMustChangePassword' | 'logout'>

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      ...initialState,
      setTokens: (tokens, role, username) =>
        set({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          mustChangePassword: tokens.mustChangePassword,
          role,
          username,
          pendingTotpTicket: null,
        }),
      setPendingTotp: (loginTicketId, role, username) =>
        set({ pendingTotpTicket: loginTicketId, role, username }),
      setMustChangePassword: (value) => set({ mustChangePassword: value }),
      logout: () => set({ ...initialState }),
    }),
    {
      name: 'exhibitor-auth',
      storage: createJSONStorage(() => window.localStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        role: state.role,
        username: state.username,
        mustChangePassword: state.mustChangePassword,
      }),
    },
  ),
)
