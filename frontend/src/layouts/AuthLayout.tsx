import { Box, Container, Paper, Stack, Typography } from '@mui/material'
import { Outlet } from 'react-router-dom'

export default function AuthLayout() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
      }}
    >
      <Container maxWidth="xs">
        <Stack spacing={3} sx={{ alignItems: 'center' }}>
          <Typography variant="h5" component="h1" color="primary" sx={{ fontWeight: 700 }}>
            Exhibitor Registration System
          </Typography>
          <Paper sx={{ p: 4, width: '100%' }}>
            <Outlet />
          </Paper>
        </Stack>
      </Container>
    </Box>
  )
}
