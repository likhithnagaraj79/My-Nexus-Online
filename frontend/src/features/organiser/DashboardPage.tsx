import { Alert, Grid, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { getDashboard } from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'
import StatCard from '../shared/StatCard'

export default function DashboardPage() {
  const query = useQuery({ queryKey: ['organiser-dashboard'], queryFn: getDashboard })

  return (
    <>
      <Typography variant="h5" gutterBottom>
        Dashboard
      </Typography>
      {query.isError && <Alert severity="error">{extractErrorMessage(query.error)}</Alert>}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Printed Badges" value={query.data?.printedBadgeCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Issued Badges" value={query.data?.issuedBadgeCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Submissions" value={query.data?.submissionCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Exhibitor People" value={query.data?.exhibitorPersonCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Check-ins" value={query.data?.checkInCount} loading={query.isLoading} />
        </Grid>
      </Grid>
    </>
  )
}
