import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../../test/renderWithProviders'
import { useAuthStore } from '../../store/authStore'
import LoginPage from './LoginPage'

async function fillAndSubmit(username: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Username'), username)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: /sign in/i }))
}

describe('LoginPage', () => {
  it('logs in successfully and stores tokens', async () => {
    renderWithProviders(<LoginPage />)

    await fillAndSubmit('admin', 'correct-password')

    await waitFor(() => {
      expect(useAuthStore.getState().accessToken).toBe('access-1')
    })
  })

  it('routes to the TOTP step when the backend requires it', async () => {
    renderWithProviders(<LoginPage />)

    await fillAndSubmit('totpuser', 'correct-password')

    await waitFor(() => {
      expect(useAuthStore.getState().pendingTotpTicket).toBe('ticket-1')
    })
    expect(useAuthStore.getState().accessToken).toBeNull()
  })

  it('shows a lockout message on 423', async () => {
    renderWithProviders(<LoginPage />)

    await fillAndSubmit('locked', 'whatever')

    expect(await screen.findByText(/locked after too many failed attempts/i)).toBeInTheDocument()
  })
})
