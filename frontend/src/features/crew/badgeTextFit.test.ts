import { describe, expect, it } from 'vitest'
import { fitFontSizePt } from './badgeTextFit'

describe('fitFontSizePt', () => {
  it('returns the configured size unchanged when canvas measurement is unavailable (e.g. jsdom)', () => {
    // jsdom does not implement a real 2D canvas context, so this exercises the same
    // fail-safe fallback path a headless/older browser would hit — never shrink blindly.
    expect(fitFontSizePt('Any Text', 24, true)).toBe(24)
  })

  it('never returns less than the configured size for short text', () => {
    expect(fitFontSizePt('Jane Doe', 20, false)).toBe(20)
  })
})
