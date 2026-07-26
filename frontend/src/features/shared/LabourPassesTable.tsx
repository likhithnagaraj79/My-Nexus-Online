import { Alert, Chip, Paper, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useQuery } from '@tanstack/react-query'
import { listLabourPasses, type LabourPassSummary } from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'

const PASS_TYPE_LABEL: Record<LabourPassSummary['passType'], string> = {
  VENDOR: 'Vendor',
  EXHIBITOR: 'Exhibitor',
  FABRICATOR_LABOUR: 'Fabricator / Labour',
}

const columns: GridColDef<LabourPassSummary>[] = [
  {
    field: 'passType',
    headerName: 'Type',
    width: 180,
    valueGetter: (_value, row) => PASS_TYPE_LABEL[row.passType],
  },
  { field: 'passCount', headerName: 'Passes', width: 100, type: 'number' },
  { field: 'phoneNumber', headerName: 'Phone', width: 150 },
  { field: 'stallNumber', headerName: 'Stall', width: 120, valueGetter: (_v, row) => row.stallNumber ?? '—' },
  { field: 'issuedByUsername', headerName: 'Issued By', width: 150 },
  {
    field: 'createdAt',
    headerName: 'Created',
    width: 200,
    valueFormatter: (value: string) => new Date(value).toLocaleString(),
  },
]

/** Read-only list shared between Admin (/admin/labour-passes) and Organiser
 * (/organiser/labour-passes) — both roles hit the same backend endpoint. */
export default function LabourPassesTable() {
  const query = useQuery({ queryKey: ['labour-passes'], queryFn: listLabourPasses })

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Labour Passes
      </Typography>
      {query.isError && <Alert severity="error">{extractErrorMessage(query.error)}</Alert>}
      <div style={{ height: 520, width: '100%' }}>
        <DataGrid rows={query.data ?? []} columns={columns} loading={query.isLoading} getRowId={(row) => row.id} />
      </div>
    </Paper>
  )
}

// Re-export so callers can render a status chip if they want it elsewhere.
export function LabourPassTypeChip({ type }: { type: LabourPassSummary['passType'] }) {
  return <Chip label={PASS_TYPE_LABEL[type]} size="small" />
}
