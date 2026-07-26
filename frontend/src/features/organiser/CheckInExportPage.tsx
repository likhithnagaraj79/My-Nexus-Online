import DownloadIcon from '@mui/icons-material/Download'
import { Alert, Button, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getActiveEvent, getEventDays } from '../../api/common'
import { exportCheckIns } from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'
import { downloadBlob } from '../../lib/downloadBlob'

export default function CheckInExportPage() {
  const [eventDayId, setEventDayId] = useState('')

  const eventQuery = useQuery({ queryKey: ['active-event'], queryFn: getActiveEvent })

  const daysQuery = useQuery({
    queryKey: ['event-days', eventQuery.data?.id],
    queryFn: () => getEventDays(eventQuery.data!.id),
    enabled: !!eventQuery.data,
  })

  const mutation = useMutation({
    mutationFn: () => exportCheckIns(eventDayId),
    onSuccess: (blob) => downloadBlob(blob, `check-ins-${eventDayId}.csv`),
  })

  return (
    <>
      <Typography variant="h5" gutterBottom>
        Check-in Export
      </Typography>

      {eventQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(eventQuery.error)}
        </Alert>
      )}
      {mutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(mutation.error)}
        </Alert>
      )}

      {eventQuery.data && (
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
          <Button
            variant="contained"
            startIcon={<DownloadIcon />}
            disabled={!eventDayId || mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? 'Exporting…' : 'Export CSV'}
          </Button>
        </Stack>
      )}
    </>
  )
}
