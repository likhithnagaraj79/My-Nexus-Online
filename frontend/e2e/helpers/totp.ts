import { createHmac } from 'node:crypto'
import { Client } from 'pg'

const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
const TIME_STEP_SECONDS = 30
const CODE_DIGITS = 6

function decodeBase32(secret: string): Buffer {
  const cleaned = secret.toUpperCase().replace(/[^A-Z2-7]/g, '')
  let bits = ''
  for (const char of cleaned) {
    const index = BASE32_ALPHABET.indexOf(char)
    bits += index.toString(2).padStart(5, '0')
  }
  const bytes: number[] = []
  for (let i = 0; i + 8 <= bits.length; i += 8) {
    bytes.push(parseInt(bits.slice(i, i + 8), 2))
  }
  return Buffer.from(bytes)
}

/**
 * RFC 6238 TOTP, hand-rolled with Node's built-in crypto rather than a library — otplib 13's
 * plugin-based crypto API was too much of an unknown to wire up reliably here, and TOTP itself
 * is a small, well-specified algorithm. Matches the backend's dev.samstevens.totp defaults
 * (confirmed by reading TotpService.buildQrPngBase64): SHA1, 6 digits, 30-second step.
 */
export function generateTotpCode(base32Secret: string, atUnixSeconds: number = Math.floor(Date.now() / 1000)): string {
  const key = decodeBase32(base32Secret)
  const counter = Math.floor(atUnixSeconds / TIME_STEP_SECONDS)

  const counterBuffer = Buffer.alloc(8)
  counterBuffer.writeUInt32BE(Math.floor(counter / 2 ** 32), 0)
  counterBuffer.writeUInt32BE(counter >>> 0, 4)

  const digest = createHmac('sha1', key).update(counterBuffer).digest()
  const offset = digest[digest.length - 1] & 0xf
  const binary =
    ((digest[offset] & 0x7f) << 24) |
    ((digest[offset + 1] & 0xff) << 16) |
    ((digest[offset + 2] & 0xff) << 8) |
    (digest[offset + 3] & 0xff)

  const code = (binary % 10 ** CODE_DIGITS).toString().padStart(CODE_DIGITS, '0')
  return code
}

function pgConfig() {
  return {
    host: process.env.E2E_PG_HOST ?? 'localhost',
    port: Number(process.env.E2E_PG_PORT ?? 5432),
    user: process.env.E2E_PG_APP_USER ?? 'exhibitor_app',
    password: process.env.E2E_PG_APP_PASSWORD,
    database: 'exhibitor_registration_e2e',
  }
}

/**
 * Reads the live totp_secret for `username` directly from the e2e Postgres database and
 * computes a fresh code — queried right before each TOTP submission (not cached), since every
 * E2E run mints brand-new secrets for its freshly-created Crew/Validator accounts.
 */
export async function getTotpCode(username: string): Promise<string> {
  const client = new Client(pgConfig())
  await client.connect()
  try {
    const result = await client.query<{ totp_secret: string | null }>(
      'SELECT totp_secret FROM users WHERE username = $1',
      [username],
    )
    const secret = result.rows[0]?.totp_secret
    if (!secret) {
      throw new Error(`No totp_secret found for user "${username}"`)
    }
    return generateTotpCode(secret)
  } finally {
    await client.end()
  }
}
