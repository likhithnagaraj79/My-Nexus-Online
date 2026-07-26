import { Alert, Chip, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { listExhibitorPasses, type ExhibitorPassSummary } from '../../api/crew'
import { extractErrorMessage } from '../../api/client'

const TRISTATE_OPTIONS: { value: '' | 'true' | 'false'; label: string }[] = [
  { value: '', label: 'Any' },
  { value: 'true', label: 'Yes' },
  { value: 'false', label: 'No' },
]

const columns: GridColDef<ExhibitorPassSummary>[] = [
  { field: 'name', headerName: 'Name', flex: 1, minWidth: 150 },
  { field: 'designation', headerName: 'Designation', flex: 1, minWidth: 140 },
  { field: 'companyName', headerName: 'Company', flex: 1, minWidth: 160 },
  {
    field: 'printed',
    headerName: 'Printed',
    width: 110,
    renderCell: (params) =>
      params.value ? <Chip label="Printed" color="info" size="small" /> : <Chip label="Not Printed" size="small" />,
  },
  {
    field: 'issued',
    headerName: 'Issued',
    width: 110,
    renderCell: (params) =>
      params.value ? <Chip label="Issued" color="success" size="small" /> : <Chip label="Not Issued" size="small" />,
  },
]

/** Read-only for Organiser — everyone who submitted the public registration form, with their
 * printed/issued status. Same data Crew works from at /crew/exhibitor-passes, minus the
 * selection/print/issue actions, which stay Crew-only. */
export default function ExhibitorSubmissionsPage() {
  const [q, setQ] = useState('')
  const [printedFilter, setPrintedFilter] = useState<'' | 'true' | 'false'>('')
  const [issuedFilter, setIssuedFilter] = useState<'' | 'true' | 'false'>('')

  const query = useQuery({
    queryKey: ['exhibitor-submissions', q, printedFilter, issuedFilter],
    queryFn: () =>
      listExhibitorPasses({
        q: q || undefined,
        printed: printedFilter === '' ? undefined : printedFilter === 'true',
        issued: issuedFilter === '' ? undefined : issuedFilter === 'true',
      }),
  })

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Exhibitor Submissions
      </Typography>

      <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }}>
        <TextField label="Search" value={q} onChange={(e) => setQ(e.target.value)} sx={{ minWidth: 220 }} />
        <TextField
          select
          label="Printed"
          value={printedFilter}
          onChange={(e) => setPrintedFilter(e.target.value as '' | 'true' | 'false')}
          sx={{ minWidth: 140 }}
        >
          {TRISTATE_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.label}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          label="Issued"
          value={issuedFilter}
          onChange={(e) => setIssuedFilter(e.target.value as '' | 'true' | 'false')}
          sx={{ minWidth: 140 }}
        >
          {TRISTATE_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.label}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(query.error)}
        </Alert>
      )}

      <div style={{ height: 560, width: '100%' }}>
        <DataGrid rows={query.data ?? []} columns={columns} loading={query.isLoading} getRowId={(row) => row.id} />
      </div>
    </Paper>
  )
}
