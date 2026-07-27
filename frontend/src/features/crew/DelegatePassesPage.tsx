import PrintIcon from '@mui/icons-material/Print'
import { Alert, Box, Button, Chip, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { DataGrid, type GridColDef, type GridRowSelectionModel } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import {
  getBadgeTemplate,
  listDelegatePasses,
  printDelegatePasses,
  type DelegatePassSummary,
  type ElementStyle,
} from '../../api/crew'
import { extractErrorMessage } from '../../api/client'
import { fitFontSizePt } from './badgeTextFit'

/** Absolutely-positions text centered on (xPercent, yPercent) of the badge — identical
 * technique to ExhibitorPassesPage's BadgeText, since Conference Delegates print through the
 * exact same shared badge template. */
function BadgeText({ style, children }: { style: ElementStyle; children: string }) {
  return (
    <div
      style={{
        position: 'absolute',
        left: `${style.xPercent}%`,
        top: `${style.yPercent}%`,
        transform: 'translate(-50%, -50%)',
        fontSize: `${style.fontSizePt}pt`,
        fontWeight: style.bold ? 700 : 400,
        whiteSpace: 'nowrap',
        fontFamily: 'Arial, sans-serif',
      }}
    >
      {children}
    </div>
  )
}

const DEAD_ZONE_TOP_PERCENT = (8.6 / 15.3) * 100
const DEAD_ZONE_BOTTOM_HEIGHT_PERCENT = (1.5 / 15.3) * 100

function fittedStyle(text: string, style: ElementStyle): ElementStyle {
  return { ...style, fontSizePt: fitFontSizePt(text, style.fontSizePt, style.bold) }
}

const TRISTATE_OPTIONS: { value: '' | 'true' | 'false'; label: string }[] = [
  { value: '', label: 'Any' },
  { value: 'true', label: 'Yes' },
  { value: 'false', label: 'No' },
]

export default function DelegatePassesPage() {
  const [q, setQ] = useState('')
  const [printedFilter, setPrintedFilter] = useState<'' | 'true' | 'false'>('')
  const [selection, setSelection] = useState<GridRowSelectionModel>({ type: 'include', ids: new Set() })
  const [printQueue, setPrintQueue] = useState<DelegatePassSummary[]>([])

  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['delegate-passes', q, printedFilter],
    queryFn: () =>
      listDelegatePasses({
        q: q || undefined,
        printed: printedFilter === '' ? undefined : printedFilter === 'true',
      }),
  })

  const templateQuery = useQuery({ queryKey: ['badge-template'], queryFn: getBadgeTemplate })

  const printMutation = useMutation({
    mutationFn: printDelegatePasses,
    onSuccess: async (printed) => {
      queryClient.invalidateQueries({ queryKey: ['delegate-passes'] })
      setSelection({ type: 'include', ids: new Set() })
      // Force a fresh fetch rather than trusting whatever's already cached under this query
      // key — Crew may have just saved a new template on another page/tab a moment ago, and
      // the printed badge must always reflect the latest saved layout, not a stale one.
      await queryClient.fetchQuery({ queryKey: ['badge-template'], queryFn: getBadgeTemplate })
      setPrintQueue(printed)
    },
  })

  // Fires the real browser print once the hidden badge pages below have committed to the DOM;
  // clears the queue once the browser reports printing is done (or cancelled) so the hidden
  // print area doesn't linger with stale content for the next print.
  useEffect(() => {
    if (printQueue.length === 0) return
    window.print()
    const clear = () => setPrintQueue([])
    window.addEventListener('afterprint', clear)
    return () => window.removeEventListener('afterprint', clear)
  }, [printQueue])

  const selectedIds = useMemo(() => Array.from(selection.ids), [selection])

  const columns: GridColDef<DelegatePassSummary>[] = [
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
  ]

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Conference Delegates
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
      </Stack>

      {query.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(query.error)}
        </Alert>
      )}
      {printMutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(printMutation.error)}
        </Alert>
      )}

      {selectedIds.length > 0 && (
        <Box sx={{ mb: 2, display: 'flex', gap: 2, alignItems: 'center' }}>
          <Typography variant="body2">{selectedIds.length} selected</Typography>
          <Button
            size="small"
            variant="outlined"
            startIcon={<PrintIcon />}
            disabled={printMutation.isPending}
            onClick={() => printMutation.mutate({ personIds: selectedIds.map(String) })}
          >
            Print Selected
          </Button>
        </Box>
      )}

      <div style={{ height: 560, width: '100%' }}>
        <DataGrid
          rows={query.data ?? []}
          columns={columns}
          loading={query.isLoading}
          checkboxSelection
          rowSelectionModel={selection}
          onRowSelectionModelChange={setSelection}
        />
      </div>

      {/* Portaled to #badge-print-portal (a sibling of #root, see index.html) rather than
          rendered here inline — printing hides #root entirely via display:none, which only
          works because this tree lives outside it. Hidden on screen (index.css), visible only
          under @media print — one physical 10.1cm x 15.3cm page per delegate just printed,
          positioned per the same shared badge template used for exhibitors. No QR code: the
          printed badge only ever shows Name/Designation/Company. */}
      {templateQuery.data &&
        createPortal(
          <div id="badge-print-area">
            {printQueue.map((person) => (
              <div key={person.id} className="badge-page">
                <div className="badge-dead-zone" style={{ top: 0, height: `${DEAD_ZONE_TOP_PERCENT}%` }} />
                <div
                  className="badge-dead-zone"
                  style={{ bottom: 0, height: `${DEAD_ZONE_BOTTOM_HEIGHT_PERCENT}%` }}
                />
                <BadgeText style={fittedStyle(person.name, templateQuery.data.name)}>{person.name}</BadgeText>
                <BadgeText style={fittedStyle(person.designation, templateQuery.data.designation)}>
                  {person.designation}
                </BadgeText>
                <BadgeText style={fittedStyle(person.companyName, templateQuery.data.company)}>
                  {person.companyName}
                </BadgeText>
              </div>
            ))}
          </div>,
          document.getElementById('badge-print-portal')!,
        )}
    </Paper>
  )
}
