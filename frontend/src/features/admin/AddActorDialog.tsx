import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { createCrew, createOrganiser, createValidator, type CreatedActorResponse } from '../../api/admin'
import { extractErrorMessage } from '../../api/client'

const schema = z
  .object({
    actorRole: z.enum(['ORGANISER', 'CREW', 'VALIDATOR']),
    username: z.string().min(1, 'Username is required'),
    email: z.string().optional(),
    temporaryPassword: z.string().min(8, 'Password must be at least 8 characters'),
    aadharNumber: z.string().optional(),
    phoneNumber: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.actorRole === 'ORGANISER') {
      if (!data.email || !/^\S+@\S+\.\S+$/.test(data.email)) {
        ctx.addIssue({ code: 'custom', path: ['email'], message: 'Enter a valid email' })
      }
    } else {
      if (!data.aadharNumber || !/^\d{12}$/.test(data.aadharNumber)) {
        ctx.addIssue({ code: 'custom', path: ['aadharNumber'], message: 'Must be a 12-digit Aadhar number' })
      }
      if (!data.phoneNumber) {
        ctx.addIssue({ code: 'custom', path: ['phoneNumber'], message: 'Phone number is required' })
      }
    }
  })

type FormValues = z.infer<typeof schema>

interface AddActorDialogProps {
  open: boolean
  onClose: () => void
  onCreated: (response: CreatedActorResponse) => void
}

export default function AddActorDialog({ open, onClose, onCreated }: AddActorDialogProps) {
  const queryClient = useQueryClient()

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      actorRole: 'ORGANISER',
      username: '',
      email: '',
      temporaryPassword: '',
      aadharNumber: '',
      phoneNumber: '',
    },
  })

  const actorRole = useWatch({ control, name: 'actorRole' })

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (values.actorRole === 'ORGANISER') {
        return createOrganiser({
          username: values.username,
          email: values.email!,
          temporaryPassword: values.temporaryPassword,
        })
      }
      const body = {
        username: values.username,
        temporaryPassword: values.temporaryPassword,
        aadharNumber: values.aadharNumber!,
        phoneNumber: values.phoneNumber!,
      }
      return values.actorRole === 'CREW' ? createCrew(body) : createValidator(body)
    },
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['actors'] })
      reset()
      onCreated(response)
    },
  })

  const handleClose = () => {
    reset()
    mutation.reset()
    onClose()
  }

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add Actor</DialogTitle>
      <DialogContent>
        <Stack spacing={2} component="form" id="add-actor-form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 1 }}>
          {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

          <Controller
            name="actorRole"
            control={control}
            render={({ field }) => (
              <TextField {...field} select label="Role">
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
              <TextField {...field} label="Username" error={!!errors.username} helperText={errors.username?.message} />
            )}
          />

          {actorRole === 'ORGANISER' ? (
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <TextField {...field} label="Email" error={!!errors.email} helperText={errors.email?.message} />
              )}
            />
          ) : (
            <>
              <Controller
                name="aadharNumber"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Aadhar number"
                    error={!!errors.aadharNumber}
                    helperText={errors.aadharNumber?.message}
                  />
                )}
              />
              <Controller
                name="phoneNumber"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Phone number"
                    error={!!errors.phoneNumber}
                    helperText={errors.phoneNumber?.message}
                  />
                )}
              />
            </>
          )}

          <Controller
            name="temporaryPassword"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                type="password"
                label="Temporary password"
                error={!!errors.temporaryPassword}
                helperText={errors.temporaryPassword?.message}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button type="submit" form="add-actor-form" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating…' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
