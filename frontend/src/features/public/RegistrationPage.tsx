import { zodResolver } from '@hookform/resolvers/zod'
import DeleteIcon from '@mui/icons-material/Delete'
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import axios from 'axios'
import { useState } from 'react'
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form'
import ReCAPTCHA from 'react-google-recaptcha'
import { useParams } from 'react-router-dom'
import { z } from 'zod'
import { extractErrorMessage } from '../../api/client'
import { getLinkInfo, searchCompanies, submitRegistration, type CompanySuggestion } from '../../api/public'

const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY as string

const personSchema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  designation: z.string().min(1, 'Designation is required').max(150),
})

const schema = z.object({
  companyName: z.string().min(1, 'Company name is required').max(200),
  numExhibitors: z.number().min(1).max(9),
  people: z.array(personSchema).min(1).max(9),
  recaptchaToken: z.string().min(1, 'Please complete the reCAPTCHA'),
})

type FormValues = z.infer<typeof schema>

function makePeople(count: number, existing: FormValues['people']): FormValues['people'] {
  const next = [...existing]
  while (next.length < count) next.push({ name: '', designation: '' })
  next.length = count
  return next
}

export default function RegistrationPage() {
  const { linkId } = useParams<{ linkId: string }>()
  const [companyInput, setCompanyInput] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const linkQuery = useQuery({
    queryKey: ['public-link', linkId],
    queryFn: () => getLinkInfo(linkId!),
    enabled: !!linkId,
    retry: false,
  })

  const companiesQuery = useQuery({
    queryKey: ['public-companies', companyInput],
    queryFn: () => searchCompanies(companyInput),
    enabled: companyInput.trim().length >= 2,
  })

  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      companyName: '',
      numExhibitors: 1,
      people: [{ name: '', designation: '' }],
      recaptchaToken: '',
    },
  })

  const { fields, replace } = useFieldArray({ control, name: 'people' })
  const numExhibitors = useWatch({ control, name: 'numExhibitors' })
  const people = useWatch({ control, name: 'people' })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      submitRegistration(linkId!, {
        companyName: values.companyName,
        people: values.people,
        recaptchaToken: values.recaptchaToken,
      }),
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

      <Stack spacing={2} component="form" onSubmit={handleSubmit(onSubmit)}>
        {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

        <Controller
          name="companyName"
          control={control}
          render={({ field }) => (
            <Autocomplete
              freeSolo
              options={(companiesQuery.data ?? []).map((c: CompanySuggestion) => c.name)}
              inputValue={field.value}
              onInputChange={(_event, value) => {
                field.onChange(value)
                setCompanyInput(value)
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Company Name"
                  error={!!errors.companyName}
                  helperText={errors.companyName?.message}
                />
              )}
            />
          )}
        />

        <Controller
          name="numExhibitors"
          control={control}
          render={({ field }) => (
            <TextField
              {...field}
              select
              label="Number of Exhibitors"
              value={field.value}
              onChange={(e) => {
                const count = Number(e.target.value)
                field.onChange(count)
                const nextPeople = makePeople(count, people)
                replace(nextPeople)
                setValue('people', nextPeople)
              }}
            >
              {Array.from({ length: 9 }, (_, i) => i + 1).map((n) => (
                <MenuItem key={n} value={n}>
                  {n}
                </MenuItem>
              ))}
            </TextField>
          )}
        />

        <Divider />

        {fields.map((field, index) => (
          <Stack key={field.id} direction="row" spacing={2}>
            <Controller
              name={`people.${index}.name`}
              control={control}
              render={({ field: nameField }) => (
                <TextField
                  {...nameField}
                  label={`Exhibitor ${index + 1} Name`}
                  fullWidth
                  error={!!errors.people?.[index]?.name}
                  helperText={errors.people?.[index]?.name?.message}
                />
              )}
            />
            <Controller
              name={`people.${index}.designation`}
              control={control}
              render={({ field: designationField }) => (
                <TextField
                  {...designationField}
                  label="Designation"
                  fullWidth
                  error={!!errors.people?.[index]?.designation}
                  helperText={errors.people?.[index]?.designation?.message}
                />
              )}
            />
            {numExhibitors > 1 && (
              <IconButton
                aria-label="Remove exhibitor"
                onClick={() => {
                  const count = numExhibitors - 1
                  setValue('numExhibitors', count)
                  const nextPeople = people.filter((_, i) => i !== index)
                  replace(nextPeople)
                  setValue('people', nextPeople)
                }}
              >
                <DeleteIcon />
              </IconButton>
            )}
          </Stack>
        ))}

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
