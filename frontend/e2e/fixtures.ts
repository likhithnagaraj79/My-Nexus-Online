import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { getTotpCode } from './helpers/totp'

/** Plain helper functions rather than Playwright's test.extend fixtures — the golden-path spec
 * is a single test.describe.serial block sharing ONE page/session across role switches (just
 * like one operator switching roles in a browser), so per-test fixture injection doesn't fit as
 * well as a handful of reusable "log in as X" functions called at each step. */

export async function loginSingleStep(
  page: Page,
  role: 'ADMIN' | 'ORGANISER',
  username: string,
  password: string,
) {
  await page.goto('/login')
  await page.getByLabel('Role').click()
  await page.getByRole('option', { name: role === 'ADMIN' ? 'Admin' : 'Organiser' }).click()
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()
}

export async function loginTwoStep(
  page: Page,
  role: 'CREW' | 'VALIDATOR',
  username: string,
  password: string,
) {
  await page.goto('/login')
  await page.getByLabel('Role').click()
  await page.getByRole('option', { name: role === 'CREW' ? 'Crew' : 'Validator' }).click()
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()

  await expect(page.getByText('Two-factor authentication')).toBeVisible()
  const code = await getTotpCode(username)
  await page.getByLabel('Authentication code').fill(code)
  await page.getByRole('button', { name: /verify/i }).click()
}

/** Reads the current session's access token out of the persisted Zustand auth store
 * (localStorage key "exhibitor-auth", zustand persist's default `{state, version}` shape). */
export async function getAccessToken(page: Page): Promise<string> {
  const token = await page.evaluate(() => {
    const raw = localStorage.getItem('exhibitor-auth')
    return raw ? (JSON.parse(raw).state?.accessToken as string | undefined) : undefined
  })
  if (!token) throw new Error('No access token found in the page session')
  return token
}

export async function changePassword(page: Page, currentPassword: string, newPassword: string) {
  await expect(page.getByText('Change your password')).toBeVisible()
  await page.getByLabel('Current password').fill(currentPassword)
  await page.getByLabel('New password', { exact: true }).fill(newPassword)
  await page.getByLabel('Confirm new password').fill(newPassword)
  await page.getByRole('button', { name: /change password/i }).click()
}
