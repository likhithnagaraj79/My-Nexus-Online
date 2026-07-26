import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { createEvent } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

const schema = z
  .object({
    name: z.string().min(1, 'Name is required'),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
  })
  .refine((data) => data.endDate >= data.startDate, {
    message: 'End date must not be before start date',
    path: ['endDate'],
  })

type FormValues = z.infer<typeof schema>

export default function CreateEventDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient()

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { name: '', startDate: '', endDate: '' } })

  const mutation = useMutation({
    mutationFn: createEvent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] })
      reset()
      onClose()
    },
  })

  const handleClose = () => {
    reset()
    mutation.reset()
    onClose()
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle>Create Event</DialogTitle>
      <DialogContent>
        <Stack spacing={2} component="form" id="create-event-form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 1 }}>
          {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}
          <Controller
            name="name"
            control={control}
            render={({ field }) => (
              <TextField {...field} label="Name" error={!!errors.name} helperText={errors.name?.message} />
            )}
          />
          <Controller
            name="startDate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Start date"
                type="date"
                slotProps={{ inputLabel: { shrink: true } }}
                error={!!errors.startDate}
                helperText={errors.startDate?.message}
              />
            )}
          />
          <Controller
            name="endDate"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="End date"
                type="date"
                slotProps={{ inputLabel: { shrink: true } }}
                error={!!errors.endDate}
                helperText={errors.endDate?.message}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button type="submit" form="create-event-form" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating…' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
