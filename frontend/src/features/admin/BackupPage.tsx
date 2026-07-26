import DownloadIcon from '@mui/icons-material/Download'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import { Alert, Box, Button, Divider, Paper, Stack, TextField, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { exportBackup, restoreBackup } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'
import { downloadBlob } from '../../lib/downloadBlob'

const CONFIRM_TEXT = 'RESTORE'

export default function BackupPage() {
  const [file, setFile] = useState<File | null>(null)
  const [confirmText, setConfirmText] = useState('')

  const exportMutation = useMutation({
    mutationFn: exportBackup,
    onSuccess: (blob) => downloadBlob(blob, `exhibitor-registration-backup-${Date.now()}.dump`),
  })

  const restoreMutation = useMutation({
    mutationFn: () => restoreBackup(file!),
    onSuccess: () => {
      setFile(null)
      setConfirmText('')
    },
  })

  const canRestore = !!file && confirmText === CONFIRM_TEXT && !restoreMutation.isPending

  return (
    <Stack spacing={3}>
      <Typography variant="h5">Backup &amp; Restore</Typography>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Export
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Downloads a full database dump.
        </Typography>
        {exportMutation.isError && <Alert severity="error">{extractErrorMessage(exportMutation.error)}</Alert>}
        <Button
          variant="contained"
          startIcon={<DownloadIcon />}
          onClick={() => exportMutation.mutate()}
          disabled={exportMutation.isPending}
        >
          {exportMutation.isPending ? 'Exporting…' : 'Export Backup'}
        </Button>
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Restore
        </Typography>
        <Alert severity="warning" sx={{ mb: 2 }}>
          This will overwrite ALL current data with the contents of the uploaded backup file. This
          cannot be undone.
        </Alert>

        {restoreMutation.isError && <Alert severity="error" sx={{ mb: 2 }}>{extractErrorMessage(restoreMutation.error)}</Alert>}

        <Stack spacing={2}>
          <Button variant="outlined" component="label" startIcon={<UploadFileIcon />} sx={{ alignSelf: 'flex-start' }}>
            {file ? file.name : 'Choose backup file'}
            <input
              type="file"
              hidden
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </Button>

          <Divider />

          <Typography variant="body2">
            Type <strong>{CONFIRM_TEXT}</strong> below to confirm you understand this is destructive.
          </Typography>
          <TextField
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder={CONFIRM_TEXT}
            sx={{ maxWidth: 300 }}
          />

          <Box>
            <Button
              variant="contained"
              color="error"
              disabled={!canRestore}
              onClick={() => restoreMutation.mutate()}
            >
              {restoreMutation.isPending ? 'Restoring…' : 'Restore Database'}
            </Button>
          </Box>
        </Stack>
      </Paper>
    </Stack>
  )
}
