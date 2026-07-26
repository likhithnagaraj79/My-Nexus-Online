import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material'
import { useState } from 'react'

interface TotpQrDialogProps {
  open: boolean
  totpQrPngBase64: string | null
  onClose: () => void
}

/** Shown once after creating (or regenerating) a Crew/Validator account. The QR can't be
 * re-shown afterward except via the explicit "regenerate" action, so this requires an
 * explicit acknowledgement before it can be dismissed. */
export default function TotpQrDialog({ open, totpQrPngBase64, onClose }: TotpQrDialogProps) {
  const [acknowledged, setAcknowledged] = useState(false)

  const handleClose = () => {
    setAcknowledged(false)
    onClose()
  }

  return (
    <Dialog open={open} onClose={() => {}} maxWidth="xs" fullWidth>
      <DialogTitle>Scan into an authenticator app</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ alignItems: 'center' }}>
          <Alert severity="warning" sx={{ width: '100%' }}>
            This QR code will not be shown again. Have the account holder scan it now with Google
            Authenticator (or a similar app) before closing this dialog.
          </Alert>
          {totpQrPngBase64 && (
            <img
              src={`data:image/png;base64,${totpQrPngBase64}`}
              alt="TOTP enrollment QR code"
              width={220}
              height={220}
            />
          )}
          <Typography
            variant="body2"
            color="text.secondary"
            component="label"
            sx={{ display: 'flex', alignItems: 'center', gap: 1, cursor: 'pointer' }}
          >
            <input
              type="checkbox"
              checked={acknowledged}
              onChange={(e) => setAcknowledged(e.target.checked)}
            />
            I have scanned this code and saved it
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={!acknowledged} variant="contained">
          Done
        </Button>
      </DialogActions>
    </Dialog>
  )
}
