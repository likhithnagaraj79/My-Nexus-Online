import { Alert, Box, CircularProgress, Dialog, DialogContent, DialogTitle } from '@mui/material'
import { exhibitorPassQrCodeUrl } from '../../api/crew'
import { extractErrorMessage } from '../../api/client'
import { useAuthenticatedBlobUrl } from '../../hooks/useAuthenticatedBlobUrl'

interface QrCodeDialogProps {
  passId: string | null
  onClose: () => void
}

export default function QrCodeDialog({ passId, onClose }: QrCodeDialogProps) {
  const { blobUrl, error, loading } = useAuthenticatedBlobUrl(
    passId ? exhibitorPassQrCodeUrl(passId) : null,
    !!passId,
  )

  return (
    <Dialog open={!!passId} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Badge QR Code</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 260 }}>
          {loading && <CircularProgress />}
          {!loading && !!error && <Alert severity="error">{extractErrorMessage(error)}</Alert>}
          {!loading && blobUrl && (
            <img src={blobUrl} alt="Exhibitor badge QR code" style={{ maxWidth: '100%', height: 'auto' }} />
          )}
        </Box>
      </DialogContent>
    </Dialog>
  )
}
