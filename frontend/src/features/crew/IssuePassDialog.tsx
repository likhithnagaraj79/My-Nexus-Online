import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from '@mui/material'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { issueExhibitorPass, type ExhibitorPassSummary } from '../../api/crew'
import { extractErrorMessage } from '../../api/client'

const schema = z.object({
  phoneNumber: z.string().min(1, 'Phone number is required'),
})

type FormValues = z.infer<typeof schema>

interface IssuePassDialogProps {
  pass: ExhibitorPassSummary | null
  onClose: () => void
}

export default function IssuePassDialog({ pass, onClose }: IssuePassDialogProps) {
  const queryClient = useQueryClient()

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { phoneNumber: '' } })

  const mutation = useMutation({
    mutationFn: (values: FormValues) => issueExhibitorPass(pass!.id, { phoneNumber: values.phoneNumber }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exhibitor-passes'] })
      reset()
      onClose()
    },
  })

  const handleClose = () => {
    reset()
    mutation.reset()
    onClose()
  }

  if (!pass) return null

  const onSubmit = (values: FormValues) => mutation.mutate(values)

  return (
    <Dialog open={!!pass} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle>Issue Badge — {pass.name}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} component="form" id="issue-pass-form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 1 }}>
          {mutation.isError && <Alert severity="error">{extractErrorMessage(mutation.error)}</Alert>}
          <Controller
            name="phoneNumber"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Phone Number"
                error={!!errors.phoneNumber}
                helperText={errors.phoneNumber?.message}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button type="submit" form="issue-pass-form" variant="contained" disabled={mutation.isPending}>
          {mutation.isPending ? 'Issuing…' : 'Issue'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
