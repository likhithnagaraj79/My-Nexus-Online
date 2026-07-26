import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../../test/mocks/server'
import { renderWithProviders } from '../../test/renderWithProviders'
import ScanPage from './ScanPage'

const BASE_URL = 'https://localhost:8443'

let capturedSuccessCallback: ((decodedText: string) => void) | null = null

vi.mock('html5-qrcode', () => {
  class FakeHtml5Qrcode {
    start(
      _cameraIdOrConfig: unknown,
      _config: unknown,
      successCallback: (decodedText: string) => void,
    ) {
      capturedSuccessCallback = successCallback
      return Promise.resolve(null)
    }
    pause() {}
    resume() {}
    stop() {
      return Promise.resolve()
    }
  }
  return { Html5Qrcode: FakeHtml5Qrcode }
})

describe('ScanPage', () => {
  it('shows a non-blocking warning (not an error) for a duplicate same-day scan', async () => {
    server.use(
      http.get(`${BASE_URL}/api/common/events/active`, () =>
        HttpResponse.json({ id: 'event-1', name: 'Expo 2026', startDate: '2026-08-01', endDate: '2026-08-03', active: true }),
      ),
      http.get(`${BASE_URL}/api/validator/event-days`, () =>
        HttpResponse.json([{ id: 'day-1', dayNumber: 1, date: '2026-08-01' }]),
      ),
      http.post(`${BASE_URL}/api/validator/scans`, () =>
        HttpResponse.json({
          checkInScanId: 'scan-1',
          exhibitorPersonId: 'person-1',
          personName: 'Jane Doe',
          companyName: 'Acme Corp',
          alreadyCheckedInToday: true,
          scannedAt: new Date().toISOString(),
        }),
      ),
    )

    const user = userEvent.setup()
    renderWithProviders(<ScanPage />)

    const daySelect = await screen.findByLabelText('Event Day')
    await user.click(daySelect)
    await user.click(await screen.findByRole('option', { name: /day 1/i }))
    await user.click(screen.getByRole('button', { name: /start scanning/i }))

    await waitFor(() => expect(capturedSuccessCallback).not.toBeNull())
    capturedSuccessCallback!('qr-payload-123')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(/already checked in today/i)
    expect(alert.className).toMatch(/colorWarning/i)

    // Scanning session stays open — no error state, "Stop Scanning" still available.
    expect(screen.getByRole('button', { name: /stop scanning/i })).toBeInTheDocument()
  })
})
