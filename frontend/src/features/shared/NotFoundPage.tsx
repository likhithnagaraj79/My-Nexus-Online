import { Box, Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Stack spacing={2} sx={{ alignItems: 'center' }}>
        <Typography variant="h3">404</Typography>
        <Typography color="text.secondary">This page does not exist.</Typography>
        <Button component={Link} to="/login" variant="contained">
          Back to login
        </Button>
      </Stack>
    </Box>
  )
}
