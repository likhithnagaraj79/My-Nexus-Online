import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Stack, TextField, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { loginTotp } from '../../api/auth'
import { extractErrorMessage } from '../../api/client'
import { useAuthStore } from '../../store/authStore'
import { ROLE_HOME } from './roleHome'

const schema = z.object({
  code: z.string().regex(/^\d{6}$/, 'Enter the 6-digit code from your authenticator app'),
})

type FormValues = z.infer<typeof schema>

export default function TotpPage() {
  const navigate = useNavigate()
  const { pendingTotpTicket, role, setTokens } = useAuthStore()

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { code: '' } })

  const mutation = useMutation({
    mutationFn: (code: string) => loginTotp({ loginTicketId: pendingTotpTicket!, code }),
    onSuccess: (data) => {
      if (data.tokens && role) {
        setTokens(data.tokens, role, useAuthStore.getState().username!)
        navigate(data.tokens.mustChangePassword ? '/change-password' : ROLE_HOME[role], { replace: true })
      }
    },
  })

  if (!pendingTotpTicket) {
    return <Navigate to="/login" replace />
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values.code)

  return (
    <Stack spacing={3} component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Typography variant="h6">Two-factor authentication</Typography>
      <Typography variant="body2" color="text.secondary">
        Enter the 6-digit code from your authenticator app.
      </Typography>

      {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

      <Controller
        name="code"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            label="Authentication code"
            fullWidth
            slotProps={{ htmlInput: { inputMode: 'numeric', maxLength: 6 } }}
            error={!!errors.code}
            helperText={errors.code?.message}
          />
        )}
      />

      <Button type="submit" variant="contained" size="large" disabled={mutation.isPending}>
        {mutation.isPending ? 'Verifying…' : 'Verify'}
      </Button>
    </Stack>
  )
}
