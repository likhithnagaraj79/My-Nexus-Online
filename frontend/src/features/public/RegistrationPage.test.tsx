import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../../test/mocks/server'
import { theme } from '../../theme/theme'
import RegistrationPage from './RegistrationPage'

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
        <MemoryRouter initialEntries={[`/register/${linkId}`]}>
          <Routes>
            <Route path="/register/:linkId" element={<RegistrationPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>,
  )
}

describe('RegistrationPage', () => {
  it('renders one name/designation field pair per exhibitor and adjusts when the count changes', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/links/link-1`, () =>
        HttpResponse.json({
          linkId: 'link-1',
          eventName: 'Expo 2026',
          eventStartDate: '2026-08-01',
          eventEndDate: '2026-08-03',
        }),
      ),
    )

    const user = userEvent.setup()
    renderAt('link-1')

    await screen.findByText('Expo 2026')
    expect(screen.getByLabelText('Exhibitor 1 Name')).toBeInTheDocument()
    expect(screen.queryByLabelText('Exhibitor 2 Name')).not.toBeInTheDocument()

    await user.click(screen.getByLabelText('Number of Exhibitors'))
    await user.click(await screen.findByRole('option', { name: '3' }))

    expect(screen.getByLabelText('Exhibitor 1 Name')).toBeInTheDocument()
    expect(screen.getByLabelText('Exhibitor 2 Name')).toBeInTheDocument()
    expect(screen.getByLabelText('Exhibitor 3 Name')).toBeInTheDocument()
  })

  it('shows an expired message for a 410 and an invalid-link message for a 404', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/links/expired-link`, () =>
        HttpResponse.json({ title: 'Gone', status: 410 }, { status: 410 }),
      ),
    )
    renderAt('expired-link')
    expect(await screen.findByText(/registration link has expired/i)).toBeInTheDocument()
  })

  it('shows an invalid-link message for a 404', async () => {
    server.use(
      http.get(`${BASE_URL}/api/public/links/missing-link`, () =>
        HttpResponse.json({ title: 'Not Found', status: 404 }, { status: 404 }),
      ),
    )
    renderAt('missing-link')
    expect(await screen.findByText(/invalid or no longer active/i)).toBeInTheDocument()
  })
})
