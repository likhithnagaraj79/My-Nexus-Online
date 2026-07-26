import { Alert, Grid, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { getAnalytics } from '../../api/organiser'
import { extractErrorMessage } from '../../api/client'
import StatCard from '../shared/StatCard'

export default function AnalyticsPage() {
  const query = useQuery({ queryKey: ['organiser-analytics'], queryFn: getAnalytics })

  return (
    <>
      <Typography variant="h5" gutterBottom>
        Analytics
      </Typography>
      {query.isError && <Alert severity="error">{extractErrorMessage(query.error)}</Alert>}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Check-ins" value={query.data?.checkInCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Vendor Passes" value={query.data?.vendorPassCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Exhibitor Passes" value={query.data?.exhibitorPassCount} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Fabricator Labour Passes"
            value={query.data?.fabricatorLabourPassCount}
            loading={query.isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Printed Exhibitor Badges"
            value={query.data?.printedExhibitorBadgeCount}
            loading={query.isLoading}
          />
        </Grid>
      </Grid>
    </>
  )
}
