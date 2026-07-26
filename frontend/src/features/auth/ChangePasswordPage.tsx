import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Stack, TextField, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { changePassword } from '../../api/auth'
import { extractErrorMessage } from '../../api/client'
import { useAuthStore } from '../../store/authStore'
import { ROLE_HOME } from './roleHome'

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z.string().min(8, 'New password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type FormValues = z.infer<typeof schema>

export default function ChangePasswordPage() {
  const navigate = useNavigate()
  const { accessToken, role, username, setTokens } = useAuthStore()

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword }),
    onSuccess: (tokens) => {
      if (role && username) {
        setTokens(tokens, role, username)
        navigate(ROLE_HOME[role], { replace: true })
      }
    },
  })

  if (!accessToken) {
    return <Navigate to="/login" replace />
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Stack spacing={3} component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Typography variant="h6">Change your password</Typography>
      <Typography variant="body2" color="text.secondary">
        You must set a new password before continuing.
      </Typography>

      {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

      <Controller
        name="currentPassword"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            type="password"
            label="Current password"
            fullWidth
            error={!!errors.currentPassword}
            helperText={errors.currentPassword?.message}
          />
        )}
      />
      <Controller
        name="newPassword"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            type="password"
            label="New password"
            fullWidth
            error={!!errors.newPassword}
            helperText={errors.newPassword?.message}
          />
        )}
      />
      <Controller
        name="confirmPassword"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            type="password"
            label="Confirm new password"
            fullWidth
            error={!!errors.confirmPassword}
            helperText={errors.confirmPassword?.message}
          />
        )}
      />

      <Button type="submit" variant="contained" size="large" disabled={mutation.isPending}>
        {mutation.isPending ? 'Saving…' : 'Change password'}
      </Button>
    </Stack>
  )
}
