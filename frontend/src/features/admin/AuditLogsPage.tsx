import { Alert, Box, Chip, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { listAuditLogs, type AuditEventType, type AuditLogEntry } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

const PAGE_SIZE = 15

const EVENT_TYPE_COLOR: Record<AuditEventType, 'success' | 'error' | 'default' | 'warning'> = {
  LOGIN_SUCCESS: 'success',
  LOGIN_FAILURE: 'error',
  LOGOUT: 'default',
  ACCOUNT_LOCKED: 'error',
  ACCOUNT_UNLOCKED: 'warning',
}

const columns: GridColDef<AuditLogEntry>[] = [
  { field: 'usernameAttempted', headerName: 'Username', width: 150, valueGetter: (_v, row) => row.usernameAttempted ?? '—' },
  {
    field: 'eventType',
    headerName: 'Event',
    width: 170,
    renderCell: (params) => <Chip label={params.value} color={EVENT_TYPE_COLOR[params.value as AuditEventType]} size="small" />,
  },
  { field: 'ipAddress', headerName: 'IP Address', width: 140, valueGetter: (_v, row) => row.ipAddress ?? '—' },
  {
    field: 'userAgent',
    headerName: 'User Agent',
    flex: 1,
    minWidth: 200,
    valueGetter: (_v, row) => row.userAgent ?? '—',
  },
  {
    field: 'occurredAt',
    headerName: 'Occurred At',
    width: 200,
    valueFormatter: (value: string) => new Date(value).toLocaleString(),
  },
]

export default function AuditLogsPage() {
  const [userId, setUserId] = useState('')
  const [eventType, setEventType] = useState<AuditEventType | ''>('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['audit-logs', userId, eventType, from, to, page],
    queryFn: () =>
      listAuditLogs({
        userId: userId || undefined,
        eventType: eventType || undefined,
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(to).toISOString() : undefined,
        page,
        size: PAGE_SIZE,
      }),
  })

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Audit Logs
      </Typography>

      <Paper sx={{ p: 2 }}>
        <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <TextField
            label="User ID"
            value={userId}
            onChange={(e) => {
              setUserId(e.target.value)
              setPage(0)
            }}
            sx={{ minWidth: 220 }}
          />
          <TextField
            select
            label="Event type"
            value={eventType}
            onChange={(e) => {
              setEventType(e.target.value as AuditEventType | '')
              setPage(0)
            }}
            sx={{ minWidth: 180 }}
          >
            <MenuItem value="">All events</MenuItem>
            <MenuItem value="LOGIN_SUCCESS">Login success</MenuItem>
            <MenuItem value="LOGIN_FAILURE">Login failure</MenuItem>
            <MenuItem value="LOGOUT">Logout</MenuItem>
            <MenuItem value="ACCOUNT_LOCKED">Account locked</MenuItem>
            <MenuItem value="ACCOUNT_UNLOCKED">Account unlocked</MenuItem>
          </TextField>
          <TextField
            label="From"
            type="date"
            slotProps={{ inputLabel: { shrink: true } }}
            value={from}
            onChange={(e) => {
              setFrom(e.target.value)
              setPage(0)
            }}
          />
          <TextField
            label="To"
            type="date"
            slotProps={{ inputLabel: { shrink: true } }}
            value={to}
            onChange={(e) => {
              setTo(e.target.value)
              setPage(0)
            }}
          />
        </Stack>

        {query.isError && <Alert severity="error">{extractErrorMessage(query.error)}</Alert>}

        <div style={{ height: 560, width: '100%' }}>
          <DataGrid
            rows={query.data?.content ?? []}
            columns={columns}
            loading={query.isLoading}
            paginationMode="server"
            rowCount={query.data?.metadata.totalElements ?? 0}
            paginationModel={{ page, pageSize: PAGE_SIZE }}
            onPaginationModelChange={(model) => setPage(model.page)}
            pageSizeOptions={[PAGE_SIZE]}
          />
        </div>
      </Paper>
    </Box>
  )
}
