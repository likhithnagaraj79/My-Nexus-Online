import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/mocks/server'
import { renderWithProviders } from '../../test/renderWithProviders'
import ExhibitorPassesPage from './ExhibitorPassesPage'

const BASE_URL = 'https://localhost:8443'

const PASSES = [
  {
    id: 'person-1',
    name: 'Jane Doe',
    designation: 'Manager',
    companyId: 'company-1',
    companyName: 'Acme Corp',
    printed: false,
    printedAt: null,
    issued: false,
    issuedAt: null,
  },
]

describe('ExhibitorPassesPage', () => {
  it('sends personIds (not companyId) when printing a selected row', async () => {
    server.use(
      http.get(`${BASE_URL}/api/crew/exhibitor-passes`, () => HttpResponse.json(PASSES)),
      http.post(`${BASE_URL}/api/crew/exhibitor-passes/print`, async ({ request }) => {
        const body = await request.json()
        expect(body).toEqual({ personIds: ['person-1'] })
        return HttpResponse.json(PASSES)
      }),
    )

    const user = userEvent.setup()
    renderWithProviders(<ExhibitorPassesPage />)

    await screen.findByText('Jane Doe')

    const grid = screen.getByRole('grid')
    const row = within(grid).getByText('Jane Doe').closest('[role="row"]') as HTMLElement
    const checkbox = within(row).getByRole('checkbox')
    await user.click(checkbox)

    await user.click(screen.getByRole('button', { name: /print selected/i }))

    await waitFor(() => expect(screen.queryByText(/1 selected/i)).not.toBeInTheDocument())
  })

  it('highlights a row as issued only after the issue mutation resolves', async () => {
    let issued = false
    server.use(
      http.get(`${BASE_URL}/api/crew/exhibitor-passes`, () => HttpResponse.json([{ ...PASSES[0], issued }])),
      http.patch(`${BASE_URL}/api/crew/exhibitor-passes/person-1/issue`, () => {
        issued = true
        return HttpResponse.json({ ...PASSES[0], issued: true, issuedAt: new Date().toISOString() })
      }),
    )

    const user = userEvent.setup()
    renderWithProviders(<ExhibitorPassesPage />)

    await screen.findByText('Jane Doe')

    const grid = screen.getByRole('grid')
    let row = within(grid).getByText('Jane Doe').closest('[role="row"]') as HTMLElement
    expect(row.className).not.toMatch(/row-issued/)

    await user.click(within(row).getByRole('menuitem', { name: /issue/i }))
    await user.type(await screen.findByLabelText('Phone Number'), '9999999999')
    await user.click(screen.getByRole('button', { name: /^issue$/i }))

    await waitFor(() => {
      row = within(screen.getByRole('grid')).getByText('Jane Doe').closest('[role="row"]') as HTMLElement
      expect(row.className).toMatch(/row-issued/)
    })
  })
})
