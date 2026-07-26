import AddIcon from '@mui/icons-material/Add'
import EventNoteIcon from '@mui/icons-material/EventNote'
import {
  Alert,
  Box,
  Button,
  Chip,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { activateEvent, listEvents, type EventDto } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'
import CreateEventDialog from './CreateEventDialog'
import EventDaysDialog from './EventDaysDialog'

export default function EventsPage() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [daysEvent, setDaysEvent] = useState<EventDto | null>(null)

  const query = useQuery({ queryKey: ['events'], queryFn: listEvents })

  const activateMutation = useMutation({
    mutationFn: activateEvent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['events'] }),
  })

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Events</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          Create Event
        </Button>
      </Box>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Activating an event automatically deactivates whichever event was previously active — only
        one event is active at a time.
      </Typography>

      <Paper>
        {query.isError && (
          <Alert severity="error" sx={{ m: 2 }}>
            {extractErrorMessage(query.error)}
          </Alert>
        )}
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Start</TableCell>
              <TableCell>End</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(query.data ?? []).map((event) => (
              <TableRow key={event.id}>
                <TableCell>{event.name}</TableCell>
                <TableCell>{event.startDate}</TableCell>
                <TableCell>{event.endDate}</TableCell>
                <TableCell>
                  <Chip
                    label={event.active ? 'Active' : 'Inactive'}
                    color={event.active ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="right">
                  <Button
                    size="small"
                    startIcon={<EventNoteIcon />}
                    onClick={() => setDaysEvent(event)}
                    sx={{ mr: 1 }}
                  >
                    Manage Days
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    disabled={event.active || activateMutation.isPending}
                    onClick={() => activateMutation.mutate(event.id)}
                  >
                    Activate
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {query.data?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5}>
                  <Typography color="text.secondary" sx={{ py: 2 }}>
                    No events yet. Create one to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      <CreateEventDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <EventDaysDialog event={daysEvent} onClose={() => setDaysEvent(null)} />
    </Box>
  )
}
