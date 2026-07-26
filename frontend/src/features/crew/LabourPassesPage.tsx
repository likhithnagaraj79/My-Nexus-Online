import { zodResolver } from '@hookform/resolvers/zod'
import AddIcon from '@mui/icons-material/Add'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { getActiveEvent } from '../../api/common'
import { createLabourPass, listLabourPasses, type LabourPassType } from '../../api/crew'
import type { LabourPassSummary } from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'

const schema = z
  .object({
    passType: z.enum(['VENDOR', 'EXHIBITOR', 'FABRICATOR_LABOUR']),
    passCount: z
      .string()
      .min(1, 'Required')
      .refine((v) => /^\d+$/.test(v) && Number(v) > 0, 'Must be a positive whole number'),
    phoneNumber: z.string().min(1, 'Phone number is required'),
    stallNumber: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.passType !== 'VENDOR' && !data.stallNumber?.trim()) {
      ctx.addIssue({
        code: 'custom',
        path: ['stallNumber'],
        message: 'Stall number is required for Exhibitor and Fabricator/Labour passes',
      })
    }
  })

type FormValues = z.infer<typeof schema>

const PASS_TYPE_LABEL: Record<LabourPassType, string> = {
  VENDOR: 'Vendor',
  EXHIBITOR: 'Exhibitor',
  FABRICATOR_LABOUR: 'Fabricator / Labour',
}

const columns: GridColDef<LabourPassSummary>[] = [
  { field: 'passType', headerName: 'Type', width: 180, valueGetter: (_v, row) => PASS_TYPE_LABEL[row.passType] },
  { field: 'passCount', headerName: 'Passes', width: 100, type: 'number' },
  { field: 'phoneNumber', headerName: 'Phone', width: 150 },
  { field: 'stallNumber', headerName: 'Stall', width: 120, valueGetter: (_v, row) => row.stallNumber ?? '—' },
  {
    field: 'createdAt',
    headerName: 'Created',
    width: 200,
    valueFormatter: (value: string) => new Date(value).toLocaleString(),
  },
]

export default function LabourPassesPage() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const queryClient = useQueryClient()

  const eventQuery = useQuery({ queryKey: ['active-event'], queryFn: getActiveEvent })

  const passesQuery = useQuery({
    queryKey: ['crew-labour-passes', eventQuery.data?.id],
    queryFn: () => listLabourPasses(eventQuery.data!.id),
    enabled: !!eventQuery.data,
  })

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { passType: 'VENDOR', passCount: '1', phoneNumber: '', stallNumber: '' },
  })

  const passType = useWatch({ control, name: 'passType' })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      createLabourPass({
        passType: values.passType,
        passCount: Number(values.passCount),
        phoneNumber: values.phoneNumber,
        stallNumber: values.stallNumber?.trim() || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['crew-labour-passes'] })
      setDialogOpen(false)
      reset()
    },
  })

  const handleClose = () => {
    setDialogOpen(false)
    mutation.reset()
    reset()
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Paper sx={{ p: 3 }}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Labour Passes</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          disabled={!eventQuery.data}
          onClick={() => setDialogOpen(true)}
        >
          New Pass
        </Button>
      </Stack>

      {eventQuery.isError && <Alert severity="error">{extractErrorMessage(eventQuery.error)}</Alert>}
      {passesQuery.isError && <Alert severity="error">{extractErrorMessage(passesQuery.error)}</Alert>}

      <div style={{ height: 520, width: '100%' }}>
        <DataGrid
          rows={passesQuery.data ?? []}
          columns={columns}
          loading={passesQuery.isLoading}
          getRowId={(row) => row.id}
        />
      </div>

      <Dialog open={dialogOpen} onClose={handleClose} maxWidth="xs" fullWidth>
        <DialogTitle>New Labour Pass</DialogTitle>
        <DialogContent>
          <Stack spacing={2} component="form" id="labour-pass-form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 1 }}>
            {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

            <Controller
              name="passType"
              control={control}
              render={({ field }) => (
                <TextField {...field} select label="Pass Type">
                  <MenuItem value="VENDOR">Vendor</MenuItem>
                  <MenuItem value="EXHIBITOR">Exhibitor</MenuItem>
                  <MenuItem value="FABRICATOR_LABOUR">Fabricator / Labour</MenuItem>
                </TextField>
              )}
            />

            <Controller
              name="passCount"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Pass Count"
                  type="number"
                  error={!!errors.passCount}
                  helperText={errors.passCount?.message}
                />
              )}
            />

            <Controller
              name="phoneNumber"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Phone Number"
                  error={!!errors.phoneNumber}
                  helperText={errors.phoneNumber?.message}
                />
              )}
            />

            {passType !== 'VENDOR' && (
              <Controller
                name="stallNumber"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Stall Number"
                    error={!!errors.stallNumber}
                    helperText={errors.stallNumber?.message}
                  />
                )}
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button type="submit" form="labour-pass-form" variant="contained" disabled={mutation.isPending}>
            {mutation.isPending ? 'Creating…' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  )
}
