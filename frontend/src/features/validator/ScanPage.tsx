import { Alert, Button, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Html5Qrcode } from 'html5-qrcode'
import { useEffect, useRef, useState } from 'react'
import { getActiveEvent } from '../../api/common'
import { extractErrorMessage } from '../../api/client'
import { listEventDays, submitScan, type ScanResponse } from '../../api/validator'

const SCANNER_ELEMENT_ID = 'validator-qr-scanner'
const RESUME_DELAY_MS = 2000

export default function ScanPage() {
  const [eventDayId, setEventDayId] = useState('')
  const [scanning, setScanning] = useState(false)
  const [lastResult, setLastResult] = useState<ScanResponse | null>(null)
  const [cameraError, setCameraError] = useState<string | null>(null)

  const scannerRef = useRef<Html5Qrcode | null>(null)
  const busyRef = useRef(false)

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

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Scan
      </Typography>

      {eventQuery.isError && <Alert severity="error">{extractErrorMessage(eventQuery.error)}</Alert>}

      {!scanning && (
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
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
    </Paper>
  )
}
