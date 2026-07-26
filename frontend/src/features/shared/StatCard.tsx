import { Card, CardContent, Skeleton, Typography } from '@mui/material'

interface StatCardProps {
  label: string
  value: number | undefined
  loading?: boolean
}

export default function StatCard({ label, value, loading }: StatCardProps) {
  return (
    <Card>
      <CardContent>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
        {loading ? (
          <Skeleton variant="text" width={80} height={48} />
        ) : (
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            {value ?? 0}
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}
