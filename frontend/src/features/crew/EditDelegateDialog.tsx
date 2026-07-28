import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { updateDelegate, type DelegatePassSummary } from '../../api/crew'
import { extractErrorMessage } from '../../api/client'

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  companyName: z.string().min(1, 'Company name is required').max(200),
  designation: z.string().min(1, 'Designation is required').max(150),
  mobileNumber: z.string().min(1, 'Mobile number is required').max(15),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address').max(255),
})

type FormValues = z.infer<typeof schema>

interface EditDelegateDialogProps {
  delegate: DelegatePassSummary | null
  onClose: () => void
}

export default function EditDelegateDialog({ delegate, onClose }: EditDelegateDialogProps) {
  const queryClient = useQueryClient()

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', companyName: '', designation: '', mobileNumber: '', email: '' },
  })

  // Re-populate the form whenever a different delegate is opened for editing — defaultValues
  // only apply once at mount, so each new row needs an explicit reset with its own data.
  useEffect(() => {
    if (delegate) {
      reset({
        name: delegate.name ?? '',
        companyName: delegate.companyName,
        designation: delegate.designation,
        mobileNumber: delegate.mobileNumber,
        email: delegate.email,
      })
    }
  }, [delegate, reset])

  const mutation = useMutation({
    mutationFn: (values: FormValues) => updateDelegate(delegate!.id, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['delegate-passes'] })
      onClose()
    },
  })

  const handleClose = () => {
    mutation.reset()
    onClose()
  }

  if (!delegate) return null

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Dialog open={!!delegate} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle>Edit Delegate</DialogTitle>
      <DialogContent>
        <Stack spacing={2} component="form" id="edit-delegate-form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 1 }}>
          {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}

          <Controller
            name="name"
            control={control}
            render={({ field }) => (
              <TextField {...field} label="Name" error={!!errors.name} helperText={errors.name?.message} />
            )}
          />
          <Controller
            name="companyName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Company Name"
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
                error={!!errors.email}
                helperText={errors.email?.message}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button type="submit" form="edit-delegate-form" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
