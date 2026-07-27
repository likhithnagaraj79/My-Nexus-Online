import FullscreenIcon from '@mui/icons-material/Fullscreen'
import FullscreenExitIcon from '@mui/icons-material/FullscreenExit'
import { Alert, Box, Button, FormControlLabel, MenuItem, Paper, Stack, Switch, TextField, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Html5Qrcode } from 'html5-qrcode'
import { useEffect, useRef, useState } from 'react'
import { getActiveEvent } from '../../api/common'
import { extractErrorMessage } from '../../api/client'
import { listEventDays, submitScan, type ScanResponse } from '../../api/validator'

const SCANNER_ELEMENT_ID = 'validator-qr-scanner'
const RESUME_DELAY_MS = 2000
const SCANNER_READY_STORAGE_KEY = 'validator-scanner-ready'

export default function ScanPage() {
  const [eventDayId, setEventDayId] = useState('')
  const [scanning, setScanning] = useState(false)
  const [lastResult, setLastResult] = useState<ScanResponse | null>(null)
  const [cameraError, setCameraError] = useState<string | null>(null)

  // "Scanner Ready" is self-reported by the Validator, not real hardware detection — most
  // QR/barcode scanners are keyboard-emulating (they just "type" the code + Enter), which is
  // indistinguishable from real typing to a webpage, so there's no reliable way to detect one is
  // actually plugged in. Persisted so it survives reloads at a dedicated kiosk terminal.
  const [scannerReady, setScannerReady] = useState(() => localStorage.getItem(SCANNER_READY_STORAGE_KEY) === 'true')
  const [kioskMode, setKioskMode] = useState(false)
  const [kioskInput, setKioskInput] = useState('')

  const scannerRef = useRef<Html5Qrcode | null>(null)
  const busyRef = useRef(false)
  const kioskContainerRef = useRef<HTMLDivElement | null>(null)
  const kioskInputRef = useRef<HTMLInputElement | null>(null)

  const eventQuery = useQuery({ queryKey: ['active-event'], queryFn: getActiveEvent })

  const daysQuery = useQuery({
    queryKey: ['validator-event-days', eventQuery.data?.id],
    queryFn: () => listEventDays(eventQuery.data!.id),
    enabled: !!eventQuery.data,
  })

  const scanMutation = useMutation({
    mutationFn: submitScan,
    onSuccess: (response) => {
      setLastResult(response)
      setCameraError(null)
    },
    onError: (error) => {
      setCameraError(extractErrorMessage(error))
      setLastResult(null)
    },
    onSettled: () => {
      setTimeout(() => {
        busyRef.current = false
        scannerRef.current?.resume()
      }, RESUME_DELAY_MS)
    },
  })

  const scanMutationRef = useRef(scanMutation.mutate)
  useEffect(() => {
    scanMutationRef.current = scanMutation.mutate
  }, [scanMutation.mutate])

  useEffect(() => {
    if (!scanning || !eventDayId) return

    const scanner = new Html5Qrcode(SCANNER_ELEMENT_ID)
    scannerRef.current = scanner

    scanner
      .start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: 250 },
        (decodedText) => {
          if (busyRef.current) return
          busyRef.current = true
          scanner.pause(true)
          scanMutationRef.current({ eventDayId, qrPayload: decodedText })
        },
        undefined,
      )
      .catch((err: unknown) => {
        setCameraError(err instanceof Error ? err.message : 'Unable to access the camera.')
      })

    return () => {
      scannerRef.current = null
      // start() can still be in flight (or have failed, e.g. no camera) when this runs — stop()
      // throws synchronously (not just a rejected promise) if the scanner never reached a
      // running/paused state, so guard on isScanning rather than relying on .catch() alone.
      if (scanner.isScanning) {
        scanner.stop().catch(() => undefined)
      }
    }
  }, [scanning, eventDayId])

  const handleStop = () => {
    setScanning(false)
    setLastResult(null)
    setCameraError(null)
  }

  useEffect(() => {
    localStorage.setItem(SCANNER_READY_STORAGE_KEY, String(scannerReady))
  }, [scannerReady])

  // The Fullscreen API, applied to a dedicated container (not the whole document), natively
  // renders only that element's subtree full-screen and hides everything else — no extra
  // CSS/JS needed to hide the app's nav/header while in Kiosk Mode.
  useEffect(() => {
    if (kioskMode && kioskContainerRef.current && !document.fullscreenElement) {
      kioskContainerRef.current.requestFullscreen().catch(() => undefined)
    }
  }, [kioskMode])

  // Covers exiting via Esc or an OS-level gesture (not just our own "Exit Kiosk Mode" button),
  // so re-entering Kiosk Mode afterwards behaves correctly either way.
  useEffect(() => {
    const handleFullscreenChange = () => {
      if (!document.fullscreenElement) {
        setKioskMode(false)
      }
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  useEffect(() => {
    if (kioskMode) kioskInputRef.current?.focus()
  }, [kioskMode, lastResult, cameraError])

  const handleKioskEnter = () => {
    const value = kioskInput.trim()
    if (!value || busyRef.current) return
    busyRef.current = true
    scanMutation.mutate({ eventDayId, qrPayload: value })
    setKioskInput('')
  }

  const handleExitKiosk = () => {
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => undefined)
    }
    setKioskMode(false)
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Scan
      </Typography>

      {eventQuery.isError && <Alert severity="error">{extractErrorMessage(eventQuery.error)}</Alert>}

      {!scanning && !kioskMode && (
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
            <TextField
              select
              label="Event Day"
              value={eventDayId}
              onChange={(e) => setEventDayId(e.target.value)}
              sx={{ minWidth: 220 }}
            >
              {(daysQuery.data ?? []).map((day) => (
                <MenuItem key={day.id} value={day.id}>
                  Day {day.dayNumber}
                </MenuItem>
              ))}
            </TextField>
            <Button variant="contained" disabled={!eventDayId} onClick={() => setScanning(true)}>
              Start Scanning
            </Button>
            <Button
              variant="outlined"
              startIcon={<FullscreenIcon />}
              disabled={!eventDayId}
              onClick={() => setKioskMode(true)}
            >
              Kiosk Mode
            </Button>
          </Stack>
          <FormControlLabel
            control={
              <Switch checked={scannerReady} onChange={(e) => setScannerReady(e.target.checked)} />
            }
            label={
              scannerReady
                ? 'Scanner Ready — external QR/barcode scanner plugged in'
                : 'Scanner not marked ready (manual entry still works)'
            }
          />
        </Stack>
      )}

      {scanning && (
        <Stack spacing={2}>
          <div id={SCANNER_ELEMENT_ID} style={{ maxWidth: 480 }} />

          {cameraError && <Alert severity="error">{cameraError}</Alert>}

          {lastResult && (
            <Alert severity={lastResult.alreadyCheckedInToday ? 'warning' : 'success'}>
              {lastResult.personName} — {lastResult.companyName}
              {lastResult.alreadyCheckedInToday ? ' (already checked in today)' : ' checked in'}
            </Alert>
          )}

          <Button variant="outlined" color="warning" onClick={handleStop} sx={{ alignSelf: 'flex-start' }}>
            Stop Scanning
          </Button>
        </Stack>
      )}

      {/* Kiosk Mode never opens the camera — it only ever reads from this text field, which
          accepts both manual typing and an external keyboard-emulating QR/barcode scanner
          identically (a scanner "types" the code + Enter, indistinguishable from a person
          typing). The position:fixed overlay also visually covers the whole viewport even if
          requestFullscreen() itself is denied/unsupported, so Kiosk Mode still works windowed. */}
      {kioskMode && (
        <Box
          ref={kioskContainerRef}
          sx={{
            position: 'fixed',
            inset: 0,
            zIndex: 1300,
            bgcolor: 'background.default',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 3,
            p: 4,
          }}
        >
          <Typography variant="h3" align="center">
            Check-In Kiosk
          </Typography>
          <Typography variant="body1" color="text.secondary" align="center">
            Scan a badge or type the code manually, then press Enter.
          </Typography>

          <TextField
            inputRef={kioskInputRef}
            value={kioskInput}
            onChange={(e) => setKioskInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleKioskEnter()
            }}
            placeholder="Scan or type code"
            sx={{ width: '100%', maxWidth: 480 }}
            slotProps={{ htmlInput: { style: { fontSize: '2rem', textAlign: 'center' } } }}
          />

          {cameraError && (
            <Alert severity="error" sx={{ width: '100%', maxWidth: 480, fontSize: '1.25rem' }}>
              {cameraError}
            </Alert>
          )}

          {lastResult && (
            <Alert
              severity={lastResult.alreadyCheckedInToday ? 'warning' : 'success'}
              sx={{ width: '100%', maxWidth: 480, fontSize: '1.25rem' }}
            >
              {lastResult.personName} — {lastResult.companyName}
              {lastResult.alreadyCheckedInToday ? ' (already checked in today)' : ' checked in'}
            </Alert>
          )}

          <Button variant="outlined" color="warning" startIcon={<FullscreenExitIcon />} onClick={handleExitKiosk}>
            Exit Kiosk Mode
          </Button>
        </Box>
      )}
    </Paper>
  )
}
