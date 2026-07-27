import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../../test/mocks/server'
import { theme } from '../../theme/theme'
import DelegateRegistrationPage from './DelegateRegistrationPage'

const BASE_URL = 'https://localhost:8443'

vi.mock('react-google-recaptcha', () => ({
  default: ({ onChange }: { onChange: (token: string | null) => void }) => (
    <button type="button" onClick={() => onChange('fake-recaptcha-token')}>
      Complete reCAPTCHA
    </button>
  ),
}))

function renderAt(linkId: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[`/register-delegate/${linkId}`]}>
          <Routes>
            <Route path="/register-delegate/:linkId" element={<DelegateRegistrationPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>,
  )
}

describe('DelegateRegistrationPage', () => {
  it('renders the flat delegate fields and submits successfully', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/delegate-links/link-1`, () =>
        HttpResponse.json({
          linkId: 'link-1',
          eventName: 'Expo 2026',
          eventStartDate: '2026-08-01',
          eventEndDate: '2026-08-03',
        }),
      ),
      http.post(`${BASE_URL}/api/public/delegate-links/link-1/submissions`, () =>
        HttpResponse.json({ delegateId: 'delegate-1' }, { status: 201 }),
      ),
    )

    const user = userEvent.setup()
    renderAt('link-1')

    await screen.findByText('Expo 2026')
    expect(screen.getByLabelText('Name')).toBeInTheDocument()
    expect(screen.getByLabelText('Company Name')).toBeInTheDocument()
    expect(screen.getByLabelText('Designation')).toBeInTheDocument()
    expect(screen.getByLabelText('Mobile Number')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Name'), 'Alice')
    await user.type(screen.getByLabelText('Company Name'), 'Acme Exhibits')
    await user.type(screen.getByLabelText('Designation'), 'Sales')
    await user.type(screen.getByLabelText('Mobile Number'), '9876543210')
    await user.type(screen.getByLabelText('Email'), 'alice@example.com')
    await user.click(screen.getByRole('button', { name: /complete recaptcha/i }))
    await user.click(screen.getByRole('button', { name: /submit registration/i }))

    expect(await screen.findByText('Registration submitted successfully.')).toBeInTheDocument()
  })

  it('rejects an invalid email address before submitting', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/delegate-links/link-2`, () =>
        HttpResponse.json({
          linkId: 'link-2',
          eventName: 'Expo 2026',
          eventStartDate: '2026-08-01',
          eventEndDate: '2026-08-03',
        }),
      ),
    )

    const user = userEvent.setup()
    renderAt('link-2')

    await screen.findByText('Expo 2026')
    await user.type(screen.getByLabelText('Name'), 'Alice')
    await user.type(screen.getByLabelText('Company Name'), 'Acme Exhibits')
    await user.type(screen.getByLabelText('Designation'), 'Sales')
    await user.type(screen.getByLabelText('Mobile Number'), '9876543210')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.click(screen.getByRole('button', { name: /complete recaptcha/i }))
    await user.click(screen.getByRole('button', { name: /submit registration/i }))

    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
  })

  it('shows an expired message for a 410', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/delegate-links/expired-link`, () =>
        HttpResponse.json({ title: 'Gone', status: 410 }, { status: 410 }),
      ),
    )
    renderAt('expired-link')
    expect(await screen.findByText(/registration link has expired/i)).toBeInTheDocument()
  })

  it('shows an invalid-link message for a 404', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/delegate-links/missing-link`, () =>
        HttpResponse.json({ title: 'Not Found', status: 404 }, { status: 404 }),
      ),
    )
    renderAt('missing-link')
    expect(await screen.findByText(/invalid or no longer active/i)).toBeInTheDocument()
  })
})
