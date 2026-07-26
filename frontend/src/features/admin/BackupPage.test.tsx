import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../../test/renderWithProviders'
import BackupPage from './BackupPage'

describe('BackupPage', () => {
  it('keeps the restore button disabled until a file is chosen and RESTORE is typed exactly', async () => {
    const user = userEvent.setup()
    renderWithProviders(<BackupPage />)

    const restoreButton = screen.getByRole('button', { name: /restore database/i })
    expect(restoreButton).toBeDisabled()

    const confirmField = screen.getByPlaceholderText('RESTORE')
    await user.type(confirmField, 'restore')
    expect(restoreButton).toBeDisabled()

    await user.clear(confirmField)
    await user.type(confirmField, 'RESTORE')
    expect(restoreButton).toBeDisabled()

    const file = new File(['dump-contents'], 'backup.dump', { type: 'application/octet-stream' })
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    await user.upload(fileInput, file)

    expect(restoreButton).toBeEnabled()
  })
})
