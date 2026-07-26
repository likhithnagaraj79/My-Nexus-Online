import { Box, Container, Paper, Stack, Typography } from '@mui/material'
import { Outlet } from 'react-router-dom'

export default function PublicLayout() {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'primary.contrastText', py: 3, mb: 4 }}>
        <Container maxWidth="sm">
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            Exhibitor Registration
          </Typography>
        </Container>
      </Box>
      <Container maxWidth="sm">
        <Stack spacing={3}>
          <Paper sx={{ p: 4 }}>
            <Outlet />
          </Paper>
        </Stack>
      </Container>
    </Box>
  )
}
