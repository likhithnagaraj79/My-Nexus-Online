import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/mocks/server'
import { renderWithProviders } from '../../test/renderWithProviders'
import DelegatePassesPage from './DelegatePassesPage'

const BASE_URL = 'https://localhost:8443'

const DELEGATES = [
  {
    id: 'delegate-1',
    name: null,
    designation: 'Attendee',
    companyName: 'CSV Import Co',
    mobileNumber: '9998887771',
    email: 'delegate1@example.com',
    printed: false,
    printedAt: null,
  },
]

describe('DelegatePassesPage', () => {
  it('shows "(no name)" for a CSV-imported delegate with a blank name', async () => {
    server.use(http.get(`${BASE_URL}/api/crew/conference-delegates`, () => HttpResponse.json(DELEGATES)))

    renderWithProviders(<DelegatePassesPage />)

    await screen.findByText('(no name)')
  })

  it('lets Crew edit a delegate to fill in a missing name, then reflects it in the grid', async () => {
    let current = DELEGATES[0]
    server.use(
      http.get(`${BASE_URL}/api/crew/conference-delegates`, () => HttpResponse.json([current])),
      http.put(`${BASE_URL}/api/crew/conference-delegates/delegate-1`, async ({ request }) => {
        const body = (await request.json()) as typeof current
        current = { ...current, ...body }
        return HttpResponse.json(current)
      }),
    )

    const user = userEvent.setup()
    renderWithProviders(<DelegatePassesPage />)

    await screen.findByText('(no name)')

    const grid = screen.getByRole('grid')
    const row = within(grid).getByText('(no name)').closest('[role="row"]') as HTMLElement
    await user.click(within(row).getByRole('menuitem', { name: /edit/i }))

    const nameField = await screen.findByLabelText('Name')
    await user.type(nameField, 'Alice')
    await user.click(screen.getByRole('button', { name: /^save$/i }))

    await waitFor(() => expect(screen.queryByText('Edit Delegate')).not.toBeInTheDocument())
    await screen.findByText('Alice')
  })
})
