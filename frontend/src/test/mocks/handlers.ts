import { http, HttpResponse } from 'msw'

const BASE_URL = 'https://localhost:8443'

export const handlers = [
  http.post(`${BASE_URL}/api/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { username: string; password: string }

    if (body.username === 'locked') {
      return HttpResponse.json(
        { title: 'Account locked', status: 423, detail: 'Too many failed attempts.' },
        { status: 423 },
      )
    }
    if (body.username === 'totpuser') {
      return HttpResponse.json({ totpRequired: true, loginTicketId: 'ticket-1', tokens: null })
    }
    if (body.password === 'wrong') {
      return HttpResponse.json(
        { title: 'Invalid credentials', status: 401, detail: 'Invalid username or password.' },
        { status: 401 },
      )
    }
    return HttpResponse.json({
      totpRequired: false,
      loginTicketId: null,
      tokens: { accessToken: 'access-1', refreshToken: 'refresh-1', mustChangePassword: false },
    })
  }),

  // Queried unconditionally by any Crew page rendering exhibitor passes (for the print
  // template) — a default here keeps unrelated tests' console output clean; tests that care
  // about the actual template values override this per-test via server.use().
  http.get(`${BASE_URL}/api/crew/badge-template`, () =>
    HttpResponse.json({
      name: { xPercent: 50, yPercent: 73.2, fontSizePt: 20, bold: true },
      designation: { xPercent: 50, yPercent: 81.7, fontSizePt: 14, bold: false },
      company: { xPercent: 50, yPercent: 64.71, fontSizePt: 24, bold: true },
    }),
  ),
]
