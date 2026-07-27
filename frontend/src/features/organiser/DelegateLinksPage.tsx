import AddIcon from '@mui/icons-material/Add'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import DownloadIcon from '@mui/icons-material/Download'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Paper,
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
import {
  createDelegateLink,
  deactivateDelegateLink,
  downloadDelegateImportTemplate,
  importDelegatesCsv,
  listDelegateLinks,
} from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'
import { downloadBlob } from '../../lib/downloadBlob'

export default function DelegateLinksPage() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [expiresAt, setExpiresAt] = useState('')
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const [importFile, setImportFile] = useState<File | null>(null)

  const queryClient = useQueryClient()

  const linksQuery = useQuery({ queryKey: ['organiser-delegate-links'], queryFn: listDelegateLinks })

  const createMutation = useMutation({
    mutationFn: () => createDelegateLink(expiresAt ? { expiresAt: new Date(expiresAt).toISOString() } : {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organiser-delegate-links'] })
      setDialogOpen(false)
      setExpiresAt('')
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deactivateDelegateLink(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['organiser-delegate-links'] }),
  })

  const templateMutation = useMutation({
    mutationFn: downloadDelegateImportTemplate,
    onSuccess: (blob) => downloadBlob(blob, 'conference-delegates-template.csv'),
  })

  const importMutation = useMutation({
    mutationFn: () => importDelegatesCsv(importFile!),
    onSuccess: () => setImportFile(null),
  })

  const handleCopy = async (id: string, url: string) => {
    await navigator.clipboard.writeText(url)
    setCopiedId(id)
    setTimeout(() => setCopiedId((current) => (current === id ? null : current)), 1500)
  }

  return (
    <>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Conference Delegate Links</Typography>
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
                  No conference delegate links yet.
                </Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>New Conference Delegate Link</DialogTitle>
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

      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h6" gutterBottom>
          Bulk Import via CSV
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Import delegates directly without a registration link. Columns must match the
          downloadable template exactly: Name, Company Name, Designation, Mobile Number, Email.
        </Typography>

        {templateMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {extractErrorMessage(templateMutation.error)}
          </Alert>
        )}
        {importMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {extractErrorMessage(importMutation.error)}
          </Alert>
        )}
        {importMutation.isSuccess && (
          <Alert severity={importMutation.data.errors.length > 0 ? 'warning' : 'success'} sx={{ mb: 2 }}>
            <Typography variant="body2">Imported {importMutation.data.importedCount} delegate(s).</Typography>
            {importMutation.data.errors.length > 0 && (
              <Box component="ul" sx={{ m: 0, pl: 2 }}>
                {importMutation.data.errors.map((err) => (
                  <li key={err.rowNumber}>
                    Row {err.rowNumber}: {err.reason}
                  </li>
                ))}
              </Box>
            )}
          </Alert>
        )}

        <Stack spacing={2} direction={{ xs: 'column', sm: 'row' }} sx={{ alignItems: { sm: 'center' } }}>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            disabled={templateMutation.isPending}
            onClick={() => templateMutation.mutate()}
          >
            {templateMutation.isPending ? 'Downloading…' : 'Download CSV Template'}
          </Button>

          <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', sm: 'block' } }} />

          <Button variant="outlined" component="label" startIcon={<UploadFileIcon />}>
            {importFile ? importFile.name : 'Choose CSV file'}
            <input type="file" accept=".csv" hidden onChange={(e) => setImportFile(e.target.files?.[0] ?? null)} />
          </Button>

          <Button
            variant="contained"
            disabled={!importFile || importMutation.isPending}
            onClick={() => importMutation.mutate()}
          >
            {importMutation.isPending ? 'Uploading…' : 'Upload'}
          </Button>
        </Stack>
      </Paper>
    </>
  )
}
