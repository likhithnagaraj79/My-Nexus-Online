import AddIcon from '@mui/icons-material/Add'
import LockOpenIcon from '@mui/icons-material/LockOpen'
import QrCode2Icon from '@mui/icons-material/QrCode2'
import ToggleOffIcon from '@mui/icons-material/ToggleOff'
import ToggleOnIcon from '@mui/icons-material/ToggleOn'
import { Alert, Box, Button, Chip, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { DataGrid, GridActionsCellItem, type GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  activateActor,
  deactivateActor,
  listActors,
  regenerateTotpQr,
  unlockActor,
  type ActorSummary,
} from '../../api/admin'
import { extractErrorMessage } from '../../api/client'
import type { Role } from '../../api/types'
import AddActorDialog from './AddActorDialog'
import TotpQrDialog from './TotpQrDialog'

const PAGE_SIZE = 10

export default function ActorsPage() {
  const queryClient = useQueryClient()
  const [roleFilter, setRoleFilter] = useState<Role | ''>('')
  const [activeFilter, setActiveFilter] = useState<'' | 'true' | 'false'>('')
  const [page, setPage] = useState(0)
  const [addOpen, setAddOpen] = useState(false)
  const [qrDialog, setQrDialog] = useState<string | null>(null)

  const query = useQuery({
    queryKey: ['actors', roleFilter, activeFilter, page],
    queryFn: () =>
      listActors({
        role: roleFilter || undefined,
        active: activeFilter === '' ? undefined : activeFilter === 'true',
        page,
        size: PAGE_SIZE,
      }),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['actors'] })

  const unlockMutation = useMutation({ mutationFn: unlockActor, onSuccess: invalidate })
  const activateMutation = useMutation({ mutationFn: activateActor, onSuccess: invalidate })
  const deactivateMutation = useMutation({ mutationFn: deactivateActor, onSuccess: invalidate })
  const regenerateMutation = useMutation({
    mutationFn: regenerateTotpQr,
    onSuccess: (response) => setQrDialog(response.totpQrPngBase64),
  })

  const columns: GridColDef<ActorSummary>[] = [
    { field: 'username', headerName: 'Username', flex: 1, minWidth: 140 },
    { field: 'email', headerName: 'Email', flex: 1, minWidth: 160, valueGetter: (_v, row) => row.email ?? '—' },
    { field: 'role', headerName: 'Role', width: 130 },
    {
      field: 'active',
      headerName: 'Active',
      width: 110,
      renderCell: (params) => (
        <Chip
          label={params.value ? 'Active' : 'Inactive'}
          color={params.value ? 'success' : 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'accountLocked',
      headerName: 'Locked',
      width: 110,
      renderCell: (params) =>
        params.value ? <Chip label="Locked" color="error" size="small" /> : null,
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 190,
      valueFormatter: (value: string) => new Date(value).toLocaleString(),
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 150,
      getActions: (params) => {
        const actions = [
          <GridActionsCellItem
            key="unlock"
            icon={<LockOpenIcon />}
            label="Unlock"
            disabled={!params.row.accountLocked}
            onClick={() => unlockMutation.mutate(params.row.id)}
          />,
        ]
        if (params.row.active) {
          actions.push(
            <GridActionsCellItem
              key="deactivate"
              icon={<ToggleOffIcon />}
              label="Deactivate"
              onClick={() => deactivateMutation.mutate(params.row.id)}
            />,
          )
        } else {
          actions.push(
            <GridActionsCellItem
              key="activate"
              icon={<ToggleOnIcon />}
              label="Activate"
              onClick={() => activateMutation.mutate(params.row.id)}
            />,
          )
        }
        if (params.row.role === 'CREW' || params.row.role === 'VALIDATOR') {
          actions.push(
            <GridActionsCellItem
              key="totp"
              icon={<QrCode2Icon />}
              label="Regenerate TOTP QR"
              onClick={() => regenerateMutation.mutate(params.row.id)}
            />,
          )
        }
        return actions
      },
    },
  ]

  return (
    <Stack spacing={3}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5">Actors</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)}>
          Add Actor
        </Button>
      </Box>

      <Paper sx={{ p: 2 }}>
        <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
          <TextField
            select
            label="Role"
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value as Role | '')
              setPage(0)
            }}
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="">All roles</MenuItem>
            <MenuItem value="ADMIN">Admin</MenuItem>
            <MenuItem value="ORGANISER">Organiser</MenuItem>
            <MenuItem value="CREW">Crew</MenuItem>
            <MenuItem value="VALIDATOR">Validator</MenuItem>
          </TextField>
          <TextField
            select
            label="Status"
            value={activeFilter}
            onChange={(e) => {
              setActiveFilter(e.target.value as '' | 'true' | 'false')
              setPage(0)
            }}
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="">All statuses</MenuItem>
            <MenuItem value="true">Active</MenuItem>
            <MenuItem value="false">Inactive</MenuItem>
          </TextField>
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

      <AddActorDialog
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onCreated={(response) => {
          setAddOpen(false)
          if (response.totpQrPngBase64) {
            setQrDialog(response.totpQrPngBase64)
          }
        }}
      />
      <TotpQrDialog open={qrDialog !== null} totpQrPngBase64={qrDialog} onClose={() => setQrDialog(null)} />
    </Stack>
  )
}
