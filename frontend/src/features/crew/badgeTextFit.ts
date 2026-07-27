const CM_TO_PX = 37.795275591 // fixed CSS unit conversion (96px = 1in = 2.54cm), independent of screen DPI
const BADGE_WIDTH_CM = 10.1
const EDGE_MARGIN_CM = 0.2 // per side — keeps long text from touching the physical page edge
const PRINTABLE_WIDTH_PX = (BADGE_WIDTH_CM - 2 * EDGE_MARGIN_CM) * CM_TO_PX
const MIN_FONT_SIZE_PT = 8

let measureCanvas: HTMLCanvasElement | undefined

function measureTextWidthPx(text: string, fontSizePt: number, bold: boolean): number | null {
  measureCanvas ??= document.createElement('canvas')
  const ctx = measureCanvas.getContext('2d')
  if (!ctx) return null
  ctx.font = `${bold ? 700 : 400} ${fontSizePt}pt Arial`
  return ctx.measureText(text).width
}

/** Badge text never wraps (`white-space: nowrap`), so an exhibitor with an unusually long
 * name/designation/company would otherwise print clipped past the badge's physical edge.
 * Shrinks fontSizePt just enough for `text` to fit the printable width — short text (the
 * common case) is returned unchanged at exactly the configured size. */
export function fitFontSizePt(text: string, fontSizePt: number, bold: boolean): number {
  const naturalWidthPx = measureTextWidthPx(text, fontSizePt, bold)
  if (naturalWidthPx === null || naturalWidthPx <= PRINTABLE_WIDTH_PX) return fontSizePt
  const scale = PRINTABLE_WIDTH_PX / naturalWidthPx
  return Math.max(fontSizePt * scale, MIN_FONT_SIZE_PT)
}
