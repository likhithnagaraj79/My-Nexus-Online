import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { login } from '../../api/auth'
import { extractErrorMessage } from '../../api/client'
import { useAuthStore } from '../../store/authStore'
import { ROLE_HOME } from './roleHome'

const schema = z.object({
  role: z.enum(['ADMIN', 'ORGANISER', 'CREW', 'VALIDATOR']),
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

type FormValues = z.infer<typeof schema>

export default function LoginPage() {
  const navigate = useNavigate()
  const { setTokens, setPendingTotp } = useAuthStore()
  const [locked, setLocked] = useState(false)

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'ADMIN', username: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data, variables) => {
      setLocked(false)
      if (data.totpRequired && data.loginTicketId) {
        setPendingTotp(data.loginTicketId, variables.role, variables.username)
        navigate('/login/totp')
        return
      }
      if (data.tokens) {
        setTokens(data.tokens, variables.role, variables.username)
        navigate(data.tokens.mustChangePassword ? '/change-password' : ROLE_HOME[variables.role], {
          replace: true,
        })
      }
    },
    onError: (error: unknown) => {
      const status = (error as { response?: { status?: number } }).response?.status
      setLocked(status === 423)
    },
  })

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Stack spacing={3} component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Typography variant="h6">Sign in</Typography>

      {mutation.isError && !locked && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}
      {locked && (
        <Alert severity="error">
          This account is locked after too many failed attempts. Contact an administrator to unlock it.
        </Alert>
      )}

      <Controller
        name="role"
        control={control}
        render={({ field }) => (
          <TextField {...field} select label="Role" fullWidth>
            <MenuItem value="ADMIN">Admin</MenuItem>
            <MenuItem value="ORGANISER">Organiser</MenuItem>
            <MenuItem value="CREW">Crew</MenuItem>
            <MenuItem value="VALIDATOR">Validator</MenuItem>
          </TextField>
        )}
      />

      <Controller
        name="username"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            label="Username"
            fullWidth
            error={!!errors.username}
            helperText={errors.username?.message}
          />
        )}
      />

      <Controller
        name="password"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            type="password"
            label="Password"
            fullWidth
            error={!!errors.password}
            helperText={errors.password?.message}
          />
        )}
      />

      <Button type="submit" variant="contained" size="large" disabled={mutation.isPending}>
        {mutation.isPending ? 'Signing in…' : 'Sign in'}
      </Button>
    </Stack>
  )
}
