import { zodResolver } from '@hookform/resolvers/zod'
import AddIcon from '@mui/icons-material/Add'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  List,
  ListItem,
  ListItemText,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { createEventDay, listEventDays, type EventDto } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

const schema = z.object({
  dayNumber: z
    .string()
    .min(1, 'Day number is required')
    .refine((v) => /^\d+$/.test(v) && Number(v) > 0, 'Must be a positive whole number'),
  date: z.string().min(1, 'Date is required'),
})

type FormValues = z.infer<typeof schema>

interface EventDaysDialogProps {
  event: EventDto | null
  onClose: () => void
}

export default function EventDaysDialog({ event, onClose }: EventDaysDialogProps) {
  const queryClient = useQueryClient()

  const daysQuery = useQuery({
    queryKey: ['event-days', event?.id],
    queryFn: () => listEventDays(event!.id),
    enabled: !!event,
  })

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { dayNumber: '1', date: '' } })

  const mutation = useMutation({
    mutationFn: (values: FormValues) => createEventDay(event!.id, { dayNumber: Number(values.dayNumber), date: values.date }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event-days', event?.id] })
      reset({ dayNumber: String((daysQuery.data?.length ?? 0) + 2), date: '' })
    },
  })

  if (!event) return null

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Dialog open={!!event} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Days for {event.name}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Event runs {event.startDate} to {event.endDate}
        </Typography>

        <List dense>
          {(daysQuery.data ?? []).map((day) => (
            <ListItem key={day.id} divider>
              <ListItemText primary={`Day ${day.dayNumber}`} secondary={day.date} />
            </ListItem>
          ))}
          {daysQuery.data?.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
              No days added yet.
            </Typography>
          )}
        </List>

        <Stack
          direction="row"
          spacing={1}
          component="form"
          id="add-day-form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ mt: 2 }}
        >
          {mutation.isError && (
            <Alert severity="error" sx={{ width: '100%' }}>
              {extractErrorMessage(mutation.error)}
            </Alert>
          )}
          <Controller
            name="dayNumber"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Day #"
                type="number"
                sx={{ width: 100 }}
                error={!!errors.dayNumber}
                helperText={errors.dayNumber?.message}
              />
            )}
          />
          <Controller
            name="date"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Date"
                type="date"
                slotProps={{ inputLabel: { shrink: true } }}
                error={!!errors.date}
                helperText={errors.date?.message}
                fullWidth
              />
            )}
          />
          <Button type="submit" variant="outlined" startIcon={<AddIcon />} disabled={mutation.isPending}>
            Add
          </Button>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
