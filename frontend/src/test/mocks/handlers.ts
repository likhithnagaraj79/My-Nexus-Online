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
]
