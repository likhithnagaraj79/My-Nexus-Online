import AddIcon from '@mui/icons-material/Add'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createLink, deactivateLink, listLinks } from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'

export default function LinksPage() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [expiresAt, setExpiresAt] = useState('')
  const [copiedId, setCopiedId] = useState<string | null>(null)

  const queryClient = useQueryClient()

  const linksQuery = useQuery({ queryKey: ['organiser-links'], queryFn: listLinks })

  const createMutation = useMutation({
    mutationFn: () => createLink(expiresAt ? { expiresAt: new Date(expiresAt).toISOString() } : {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organiser-links'] })
      setDialogOpen(false)
      setExpiresAt('')
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deactivateLink(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['organiser-links'] }),
  })

  const handleCopy = async (id: string, url: string) => {
    await navigator.clipboard.writeText(url)
    setCopiedId(id)
    setTimeout(() => setCopiedId((current) => (current === id ? null : current)), 1500)
  }

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Registration Links</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          New Link
        </Button>
      </Stack>

      {linksQuery.isError && <Alert severity="error">{extractErrorMessage(linksQuery.error)}</Alert>}

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>URL</TableCell>
            <TableCell>Expires</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {(linksQuery.data ?? []).map((link) => (
            <TableRow key={link.id} hover>
              <TableCell sx={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {link.publicUrl}
              </TableCell>
              <TableCell>{link.expiresAt ?? 'Never'}</TableCell>
              <TableCell>
                <Chip
                  size="small"
                  label={link.active ? 'Active' : 'Inactive'}
                  color={link.active ? 'success' : 'default'}
                />
              </TableCell>
              <TableCell align="right">
                <IconButton size="small" onClick={() => handleCopy(link.id, link.publicUrl)}>
                  <ContentCopyIcon fontSize="small" color={copiedId === link.id ? 'success' : 'inherit'} />
                </IconButton>
                <Button
                  size="small"
                  color="warning"
                  disabled={!link.active || deactivateMutation.isPending}
                  onClick={() => deactivateMutation.mutate(link.id)}
                >
                  Deactivate
                </Button>
              </TableCell>
            </TableRow>
          ))}
          {linksQuery.data?.length === 0 && (
            <TableRow>
              <TableCell colSpan={4}>
                <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                  No registration links yet.
                </Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>New Registration Link</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 1 }}>
            {createMutation.isError && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {extractErrorMessage(createMutation.error)}
              </Alert>
            )}
            <TextField
              label="Expires at (optional)"
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={createMutation.isPending} onClick={() => createMutation.mutate()}>
            {createMutation.isPending ? 'Creating…' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
