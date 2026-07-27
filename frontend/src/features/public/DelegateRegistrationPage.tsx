import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Box, Button, CircularProgress, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import ReCAPTCHA from 'react-google-recaptcha'
import { useParams } from 'react-router-dom'
import { z } from 'zod'
import { extractErrorMessage } from '../../api/client'
import { getDelegateLinkInfo, submitDelegateRegistration } from '../../api/public'

const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY as string

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  companyName: z.string().min(1, 'Company name is required').max(200),
  designation: z.string().min(1, 'Designation is required').max(150),
  mobileNumber: z.string().min(1, 'Mobile number is required').max(15),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address').max(255),
  recaptchaToken: z.string().min(1, 'Please complete the reCAPTCHA'),
})

type FormValues = z.infer<typeof schema>

export default function DelegateRegistrationPage() {
  const { linkId } = useParams<{ linkId: string }>()
  const [submitted, setSubmitted] = useState(false)

  const linkQuery = useQuery({
    queryKey: ['public-delegate-link', linkId],
    queryFn: () => getDelegateLinkInfo(linkId!),
    enabled: !!linkId,
    retry: false,
  })

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      companyName: '',
      designation: '',
      mobileNumber: '',
      email: '',
      recaptchaToken: '',
    },
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) => submitDelegateRegistration(linkId!, values),
    onSuccess: () => setSubmitted(true),
  })

  if (!linkQuery.data && linkQuery.isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (linkQuery.isError) {
    const status = axios.isAxiosError(linkQuery.error) ? linkQuery.error.response?.status : undefined
    const message =
      status === 410
        ? 'This registration link has expired.'
        : 'This registration link is invalid or no longer active.'
    return <Alert severity="error">{message}</Alert>
  }

  if (submitted) {
    return (
      <Stack spacing={2}>
        <Alert severity="success">Registration submitted successfully.</Alert>
        <Typography variant="body2" color="text.secondary">
          Thank you for registering for {linkQuery.data?.eventName}. You may close this page.
        </Typography>
      </Stack>
    )
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h6">{linkQuery.data?.eventName}</Typography>
        <Typography variant="body2" color="text.secondary">
          {linkQuery.data?.eventStartDate} — {linkQuery.data?.eventEndDate}
        </Typography>
      </Box>

      <Stack spacing={2} component="form" noValidate onSubmit={handleSubmit(onSubmit)}>
        {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

        <Controller
          name="name"
          control={control}
          render={({ field }) => (
            <TextField {...field} label="Name" fullWidth error={!!errors.name} helperText={errors.name?.message} />
          )}
        />

        <Controller
          name="companyName"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Company Name"
              fullWidth
              error={!!errors.companyName}
              helperText={errors.companyName?.message}
            />
          )}
        />

        <Controller
          name="designation"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Designation"
              fullWidth
              error={!!errors.designation}
              helperText={errors.designation?.message}
            />
          )}
        />

        <Controller
          name="mobileNumber"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Mobile Number"
              fullWidth
              error={!!errors.mobileNumber}
              helperText={errors.mobileNumber?.message}
            />
          )}
        />

        <Controller
          name="email"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              label="Email"
              type="email"
              fullWidth
              error={!!errors.email}
              helperText={errors.email?.message}
            />
          )}
        />

        <Controller
          name="recaptchaToken"
          control={control}
          render={({ field }) => (
            <Box>
              <ReCAPTCHA sitekey={RECAPTCHA_SITE_KEY} onChange={(token) => field.onChange(token ?? '')} />
              {errors.recaptchaToken && (
                <Typography variant="caption" color="error">
                  {errors.recaptchaToken.message}
                </Typography>
              )}
            </Box>
          )}
        />

        <Button type="submit" variant="contained" size="large" disabled={mutation.isPending}>
          {mutation.isPending ? 'Submitting…' : 'Submit Registration'}
        </Button>
      </Stack>
    </Stack>
  )
}
