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
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { listEventDays, type EventDto } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

interface EventDaysDialogProps {
  event: EventDto | null
  onClose: () => void
}

/** Read-only — Day 1/2/3 are auto-created the moment an Event is created (AdminEventService),
 * so there's nothing to add here, just the list. */
export default function EventDaysDialog({ event, onClose }: EventDaysDialogProps) {
  const daysQuery = useQuery({
    queryKey: ['event-days', event?.id],
    queryFn: () => listEventDays(event!.id),
    enabled: !!event,
  })

  if (!event) return null

  return (
    <Dialog open={!!event} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Days for {event.name}</DialogTitle>
      <DialogContent>
        {daysQuery.isError && <Alert severity="error">{extractErrorMessage(daysQuery.error)}</Alert>}

        <List dense>
          {(daysQuery.data ?? []).map((day) => (
            <ListItem key={day.id} divider>
              <ListItemText primary={`Day ${day.dayNumber}`} />
            </ListItem>
          ))}
          {daysQuery.data?.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
              No days yet.
            </Typography>
          )}
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
