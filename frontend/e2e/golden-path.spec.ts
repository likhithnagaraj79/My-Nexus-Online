import { expect, test } from '@playwright/test'
import type { Browser, Page } from '@playwright/test'
import { changePassword, getAccessToken, loginSingleStep, loginTwoStep } from './fixtures'

// page.request resolves relative URLs against playwright.config.ts's use.baseURL (the frontend
// dev server), which would otherwise serve its SPA index.html for these paths instead of
// reaching the backend — these direct API calls need the backend's own origin explicitly.
const BACKEND_URL = 'https://localhost:8443'

const ADMIN_TEMP_PASSWORD = 'E2eBootstrap123!'
const ADMIN_PASSWORD = 'E2eAdminChanged123!'
const ORGANISER_TEMP_PASSWORD = 'TempOrgPass123!'
const ORGANISER_PASSWORD = 'OrganiserChanged123!'
const CREW_TEMP_PASSWORD = 'TempCrewPass123!'
const CREW_PASSWORD = 'CrewChanged123!'
const VALIDATOR_TEMP_PASSWORD = 'TempValPass123!'
const VALIDATOR_PASSWORD = 'ValidatorChanged123!'

let publicLinkUrl: string
let submittedPersonId: string

test.describe.serial('golden path', () => {
  let browserRef: Browser
  let page: Page

  test.beforeAll(async ({ browser }) => {
    browserRef = browser
    page = await browser.newPage()
  })

  test.afterAll(async () => {
    await page.close()
  })

  test('bootstrap admin logs in and completes the forced password change', async () => {
    await loginSingleStep(page, 'ADMIN', 'e2e-admin', ADMIN_TEMP_PASSWORD)
    await changePassword(page, ADMIN_TEMP_PASSWORD, ADMIN_PASSWORD)
    await expect(page).toHaveURL(/\/admin\/actors/)
  })

  test('admin creates Organiser, Crew, and Validator accounts', async () => {
    // Organiser — no TOTP, single-step login.
    await page.getByRole('button', { name: /add actor/i }).click()
    let dialog = page.getByRole('dialog')
    await dialog.getByLabel('Role').click()
    await page.getByRole('option', { name: 'Organiser' }).click()
    await dialog.getByLabel('Username').fill('organiser1')
    await dialog.getByLabel('Email').fill('organiser1@example.com')
    await dialog.getByLabel('Temporary password').fill(ORGANISER_TEMP_PASSWORD)
    await dialog.getByRole('button', { name: /^create$/i }).click()
    await expect(page.getByText('organiser1', { exact: true })).toBeVisible()

    // Crew — TOTP QR must render and require acknowledgement before it can be dismissed.
    await page.getByRole('button', { name: /add actor/i }).click()
    dialog = page.getByRole('dialog')
    await dialog.getByLabel('Role').click()
    await page.getByRole('option', { name: 'Crew' }).click()
    await dialog.getByLabel('Username').fill('crew1')
    await dialog.getByLabel('Aadhar number').fill('123456789012')
    await dialog.getByLabel('Phone number').fill('9876500001')
    await dialog.getByLabel('Temporary password').fill(CREW_TEMP_PASSWORD)
    await dialog.getByRole('button', { name: /^create$/i }).click()

    await expect(page.getByAltText('TOTP enrollment QR code')).toBeVisible()
    await page.getByText('I have scanned this code and saved it').click()
    await page.getByRole('button', { name: /^done$/i }).click()
    await expect(page.getByText('crew1', { exact: true })).toBeVisible()

    // Validator — same TOTP enrollment pattern.
    await page.getByRole('button', { name: /add actor/i }).click()
    dialog = page.getByRole('dialog')
    await dialog.getByLabel('Role').click()
    await page.getByRole('option', { name: 'Validator' }).click()
    await dialog.getByLabel('Username').fill('validator1')
    await dialog.getByLabel('Aadhar number').fill('123456789013')
    await dialog.getByLabel('Phone number').fill('9876500002')
    await dialog.getByLabel('Temporary password').fill(VALIDATOR_TEMP_PASSWORD)
    await dialog.getByRole('button', { name: /^create$/i }).click()

    await expect(page.getByAltText('TOTP enrollment QR code')).toBeVisible()
    await page.getByText('I have scanned this code and saved it').click()
    await page.getByRole('button', { name: /^done$/i }).click()
    await expect(page.getByText('validator1', { exact: true })).toBeVisible()
  })

  test('admin creates and activates an Event with a Day', async () => {
    await page.goto('/admin/events')
    await page.getByRole('button', { name: /create event/i }).click()
    await page.getByLabel('Name').fill('E2E Expo 2026')
    await page.getByLabel('Start date').fill('2026-08-01')
    await page.getByLabel('End date').fill('2026-08-03')
    await page.getByRole('button', { name: /^create$/i }).click()
    await expect(page.getByText('E2E Expo 2026')).toBeVisible()

    await page.getByRole('button', { name: /activate/i }).click()
    // The "Activate" button disables once the event is active (EventsPage.tsx: disabled={event.active})
    // — a more precise signal than matching "Active" text, which also substring-matches "Inactive".
    await expect(page.getByRole('button', { name: /activate/i })).toBeDisabled()

    // Day 1/2/3 are auto-created the moment the event is created — nothing to add manually.
    await page.getByRole('button', { name: /manage days/i }).click()
    await expect(page.getByText('Day 1')).toBeVisible()
    await expect(page.getByText('Day 2')).toBeVisible()
    await expect(page.getByText('Day 3')).toBeVisible()
    await page.getByRole('button', { name: /^close$/i }).click()
  })

  test('organiser logs in, changes password, and generates a registration link', async () => {
    await page.getByLabel('logout').click()
    await expect(page).toHaveURL(/\/login/)

    await loginSingleStep(page, 'ORGANISER', 'organiser1', ORGANISER_TEMP_PASSWORD)
    await changePassword(page, ORGANISER_TEMP_PASSWORD, ORGANISER_PASSWORD)
    await expect(page).toHaveURL(/\/organiser\/dashboard/)

    await page.goto('/organiser/links')
    await page.getByRole('button', { name: /new link/i }).click()
    await page.getByRole('button', { name: /^create$/i }).click()

    const urlCell = page.locator('td', { hasText: '/register/' }).first()
    await expect(urlCell).toBeVisible()
    publicLinkUrl = (await urlCell.textContent())!.trim()
  })

  test('public registration submission (separate browser context, like an incognito tab)', async () => {
    const publicContext = await browserRef.newContext({ ignoreHTTPSErrors: true })
    const publicPage = await publicContext.newPage()

    try {
      await publicPage.goto(publicLinkUrl)
      await expect(publicPage.getByText('E2E Expo 2026')).toBeVisible()

      await publicPage.getByLabel('Company Name').fill('E2E Acme Corp')
      await publicPage.getByLabel('Number of Exhibitors').click()
      await publicPage.getByRole('option', { name: '1', exact: true }).click()
      await publicPage.getByLabel('Exhibitor 1 Name').fill('E2E Jane Doe')
      await publicPage.getByLabel('Designation').fill('Manager')

      // Google's published "always passes" test key — real widget, real Google iframe, but
      // deterministically succeeds without a human/image challenge. Matches VITE_RECAPTCHA_SITE_KEY.
      const recaptchaFrame = publicPage.frameLocator('iframe[title="reCAPTCHA"]').first()
      await recaptchaFrame.locator('#recaptcha-anchor').click()
      await expect(recaptchaFrame.locator('#recaptcha-anchor')).toHaveAttribute('aria-checked', 'true')

      await publicPage.getByRole('button', { name: /submit registration/i }).click()
      await expect(publicPage.getByText('Registration submitted successfully.')).toBeVisible()
    } finally {
      await publicContext.close()
    }
  })

  test('crew logs in, prints, and issues the exhibitor badge', async () => {
    // Wait for the SPA navigation to actually settle before the next login helper's page.goto —
    // otherwise a goto mid-flight can abort AppShell's async handleLogout() before it clears the
    // auth store, leaving a stale access token in localStorage for the next role's requests
    // (confirmed via a captured trace: the following TOTP submission carried the previous role's
    // Bearer token).
    await page.getByLabel('logout').click()
    await expect(page).toHaveURL(/\/login/)
    await loginTwoStep(page, 'CREW', 'crew1', CREW_TEMP_PASSWORD)
    await changePassword(page, CREW_TEMP_PASSWORD, CREW_PASSWORD)
    await expect(page).toHaveURL(/\/crew\/exhibitor-passes/)

    const row = page.locator('[role="row"]').filter({ hasText: 'E2E Jane Doe' })
    await expect(row).toBeVisible()
    submittedPersonId = (await row.getAttribute('data-id'))!

    await row.getByRole('checkbox').click()
    await page.getByRole('button', { name: /print selected/i }).click()
    await expect(page.getByText(/printed/i).first()).toBeVisible()

    await row.getByRole('menuitem', { name: /issue/i }).click()
    await page.getByLabel('Phone Number').fill('9999999999')
    await page.getByRole('button', { name: /^issue$/i }).click()

    await expect(async () => {
      const className = await row.getAttribute('class')
      expect(className).toContain('row-issued')
    }).toPass()
  })

  test('validator selects the day, and a scan followed by a duplicate scan is flagged', async () => {
    await page.getByLabel('logout').click()
    await expect(page).toHaveURL(/\/login/)
    await loginTwoStep(page, 'VALIDATOR', 'validator1', VALIDATOR_TEMP_PASSWORD)
    await changePassword(page, VALIDATOR_TEMP_PASSWORD, VALIDATOR_PASSWORD)
    await expect(page).toHaveURL(/\/validator\/scan/)

    await page.getByLabel('Event Day').click()
    await page.getByRole('option', { name: /day 1/i }).click()
    await page.getByRole('button', { name: /start scanning/i }).click()
    // Confirms the day-gated scanner UI mounted (the "Start Scanning" click worked and moved
    // past the day-picker gate). Not toBeVisible(): with no real camera, html5-qrcode's start()
    // rejects and the container never gets a <video>/<canvas> child, so it stays zero-height —
    // "hidden" per Playwright's actionability checks even though it's correctly attached.
    await expect(page.locator('#validator-qr-scanner')).toBeAttached()

    // A headless browser has no real camera to present a QR code to, so html5-qrcode's decode
    // loop can't be driven through the UI here (unlike every other step in this spec, which
    // does go through the real UI) — the same kind of deliberate, documented scope limit as the
    // recaptcha bypass elsewhere in this suite. Submits the scan directly against the same
    // authenticated session's API instead, then asserts the exact behavior the UI would show
    // (alreadyCheckedInToday flips to true on the second scan of the same badge).
    const token = await getAccessToken(page)
    const dayOption = await page
      .request.get(`${BACKEND_URL}/api/validator/event-days`, {
        headers: { Authorization: `Bearer ${token}` },
        params: { eventId: await activeEventId(page, token) },
      })
      .then((r) => r.json())
    const eventDayId = dayOption[0].id as string

    const firstScan = await page.request.post(`${BACKEND_URL}/api/validator/scans`, {
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      data: { eventDayId, qrPayload: submittedPersonId },
    })
    expect(firstScan.ok()).toBe(true)
    expect((await firstScan.json()).alreadyCheckedInToday).toBe(false)

    const secondScan = await page.request.post(`${BACKEND_URL}/api/validator/scans`, {
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      data: { eventDayId, qrPayload: submittedPersonId },
    })
    expect(secondScan.ok()).toBe(true)
    expect((await secondScan.json()).alreadyCheckedInToday).toBe(true)
  })

  test('organiser exports the check-in CSV and it contains the row', async () => {
    await page.getByLabel('logout').click()
    await expect(page).toHaveURL(/\/login/)
    await loginSingleStep(page, 'ORGANISER', 'organiser1', ORGANISER_PASSWORD)
    await expect(page).toHaveURL(/\/organiser\/dashboard/)

    await page.goto('/organiser/check-ins')
    await page.getByLabel('Event Day').click()
    await page.getByRole('option', { name: /day 1/i }).click()

    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('button', { name: /export csv/i }).click()
    const download = await downloadPromise

    const stream = await download.createReadStream()
    const chunks: Buffer[] = []
    for await (const chunk of stream!) chunks.push(chunk as Buffer)
    const csv = Buffer.concat(chunks).toString('utf-8')

    expect(csv).toContain('E2E Jane Doe')
    expect(csv).toContain('E2E Acme Corp')
  })

  test('organiser can view the exhibitor submission Crew already printed and issued', async () => {
    await page.goto('/organiser/exhibitor-submissions')
    await expect(page.getByRole('heading', { name: 'Exhibitor Submissions' })).toBeVisible()

    const row = page.locator('[role="row"]').filter({ hasText: 'E2E Jane Doe' })
    await expect(row).toBeVisible()
    await expect(row.getByText('Printed')).toBeVisible()
    await expect(row.getByText('Issued')).toBeVisible()
  })
})

async function activeEventId(page: Page, token: string): Promise<string> {
  const response = await page.request.get(`${BACKEND_URL}/api/common/events/active`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  return (await response.json()).id
}
